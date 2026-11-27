package com.example.backend.features.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.features.auth.AuthCookies;
import com.example.backend.features.auth.dto.LoginRequest;
import com.example.backend.features.auth.dto.LoginResponse;
import com.example.backend.features.auth.dto.RefreshRequest;
import com.example.backend.features.auth.dto.UsuarioSesionResponse;
import com.example.backend.features.auth.enums.TipoCliente;
import com.example.backend.features.auth.service.AuthService;
import com.example.backend.security.CurrentUserId;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;

    public AuthController(AuthService authService, AuthCookies authCookies) {
        this.authService = authService;
        this.authCookies = authCookies;
    }

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

        return conTokens(mobile, result, "Inicio de sesión correcto");
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
        return conTokens(mobile, result, "Sesión renovada");
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
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success(null, "Sesión cerrada"));
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioSesionResponse>> me(@Parameter(hidden = true) @CurrentUserId java.util.UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(authService.me(codigo), "Sesión actual"));
    }
}
