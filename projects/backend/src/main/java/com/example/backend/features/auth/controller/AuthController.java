package com.example.backend.features.auth.controller;

import com.example.backend.security.annotation.CurrentSessionId;
import com.example.backend.features.auth.service.CambioPasswordService;
import com.example.backend.features.auth.dto.CambiarPasswordRequest;
import com.example.backend.common.idioma.Mensajes;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.bitacora.service.OrigenSolicitud;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.features.auth.dto.AceptarInvitacionRequest;
import com.example.backend.features.auth.dto.ForgotPasswordRequest;
import com.example.backend.features.auth.dto.LoginRequest;
import com.example.backend.features.auth.dto.LoginResponse;
import com.example.backend.features.auth.dto.RefreshRequest;
import com.example.backend.features.auth.dto.ResetPasswordRequest;
import com.example.backend.features.auth.dto.UsuarioSesionResponse;
import com.example.backend.features.auth.enums.TipoCliente;
import com.example.backend.features.auth.service.AuthService;
import com.example.backend.features.auth.service.InvitacionService;
import com.example.backend.features.auth.service.RecuperacionService;
import com.example.backend.security.annotation.CurrentUserId;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;
    private final RecuperacionService recuperacion;
    private final CambioPasswordService cambioPassword;
    private final InvitacionService invitacion;
    private final Mensajes mensajes;

    /** Entrega la cookie `XSRF-TOKEN` (double-submit) al cliente web. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(@Parameter(hidden = true) CsrfToken token) {
        // * Leer el valor fuerza a Spring Security a escribir la cookie (token diferido).
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest http) {
        boolean mobile = esMobile(http);
        AuthService.LoginResult result = authService.login(new AuthService.LoginCommand(
                request.email(), request.password(), mobile ? TipoCliente.MOBILE : TipoCliente.WEB,
                http.getRemoteAddr(), http.getHeader("User-Agent")));

        return conTokens(mobile, result, mensajes.de("auth.exito.login"));
    }

    // * Web usa SOLO la cookie y mobile SOLO el body: mobile está exento de CSRF,
    // * así que no puede autenticarse con algo que el navegador adjunta solo.
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Parameter(hidden = true) @CookieValue(name = AuthCookies.REFRESH, required = false) String cookie,
            @RequestBody(required = false) RefreshRequest request, HttpServletRequest http) {
        boolean mobile = esMobile(http);
        AuthService.LoginResult result = authService.refresh(
                mobile ? tokenDelBody(request) : cookie, http.getRemoteAddr(), http.getHeader("User-Agent"));
        return conTokens(mobile, result, mensajes.de("auth.exito.sesionRenovada"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @CookieValue(name = AuthCookies.REFRESH, required = false) String cookie,
            @RequestBody(required = false) RefreshRequest request, HttpServletRequest http) {
        boolean mobile = esMobile(http);
        authService.logout(mobile ? tokenDelBody(request) : cookie);

        HttpHeaders headers = new HttpHeaders();
        if (!mobile) {
            authCookies.agregarBorrado(headers);
        }
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success(null, mensajes.de("auth.exito.sesionCerrada")));
    }

    private static boolean esMobile(HttpServletRequest http) {
        return "mobile".equalsIgnoreCase(http.getHeader("X-Client-Type"));
    }

    private static String tokenDelBody(RefreshRequest request) {
        return request == null ? null : request.refreshToken();
    }

    private ResponseEntity<ApiResponse<LoginResponse>> conTokens(boolean mobile, AuthService.LoginResult result,
            String mensaje) {
        if (mobile) {
            return ResponseEntity.ok(ApiResponse.success(
                    new LoginResponse(result.usuario(), result.accessToken(), result.refreshToken()), mensaje));
        }
        HttpHeaders headers = new HttpHeaders();
        authCookies.agregarTokens(headers, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success(
                new LoginResponse(result.usuario(), null, null), mensaje));
    }

    /** Responde igual exista o no la cuenta, y antes de saberlo. */
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Void>> olvidePassword(@Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest http) {
        recuperacion.solicitar(request.email(),
                new OrigenSolicitud(http.getRemoteAddr(), http.getHeader("User-Agent")));
        return ResponseEntity.ok(ApiResponse.success(null,
                mensajes.de("auth.exito.recuperacionSolicitada")));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> restablecerPassword(@Valid @RequestBody ResetPasswordRequest request) {
        recuperacion.restablecer(request.token(), request.password());
        return ResponseEntity.ok(ApiResponse.success(null, mensajes.de("auth.exito.passwordActualizada")));
    }

    /**
     * Cambia la propia contraseña. Cierra todas las sesiones, también esta: en web
     * se borran las cookies.
     */
    @PostMapping("/password/change")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @Parameter(hidden = true) @CurrentUserId java.util.UUID usuario,
            @Parameter(hidden = true) @CurrentSessionId java.util.UUID sesionActual,
            @Valid @RequestBody CambiarPasswordRequest request, HttpServletRequest http) {
        cambioPassword.cambiar(usuario, sesionActual, request.passwordActual(), request.passwordNueva());

        HttpHeaders headers = new HttpHeaders();
        if (!esMobile(http)) {
            authCookies.agregarBorrado(headers);
        }
        return ResponseEntity.ok().headers(headers)
                .body(ApiResponse.success(null, mensajes.de("auth.exito.passwordCambiada")));
    }

    /** La credencial es el token de la invitación: quien acepta no tiene contraseña. */
    @PostMapping("/invitation/accept")
    public ResponseEntity<ApiResponse<Void>> aceptarInvitacion(
            @Valid @RequestBody AceptarInvitacionRequest request) {
        invitacion.aceptar(request.token(), request.password());
        return ResponseEntity.ok(ApiResponse.success(null, mensajes.de("auth.exito.invitacionAceptada")));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioSesionResponse>> me(@Parameter(hidden = true) @CurrentUserId java.util.UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(authService.me(codigo), mensajes.de("auth.exito.sesionActual")));
    }
}
