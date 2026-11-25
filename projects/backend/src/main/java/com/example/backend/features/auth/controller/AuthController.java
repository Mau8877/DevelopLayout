package com.example.backend.features.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
        boolean mobile = "mobile".equalsIgnoreCase(http.getHeader("X-Client-Type"));
        AuthService.LoginResult result = authService.login(new AuthService.LoginCommand(
                request.email(), request.password(), mobile ? TipoCliente.MOBILE : TipoCliente.WEB,
                http.getRemoteAddr(), http.getHeader("User-Agent")));

        if (mobile) {
            return ResponseEntity.ok(ApiResponse.success(
                    new LoginResponse(result.usuario(), result.accessToken(), result.refreshToken()),
                    "Inicio de sesión correcto"));
        }
        HttpHeaders headers = new HttpHeaders();
        authCookies.agregarTokens(headers, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success(
                new LoginResponse(result.usuario(), null, null), "Inicio de sesión correcto"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioSesionResponse>> me(@Parameter(hidden = true) @CurrentUserId java.util.UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(authService.me(codigo), "Sesión actual"));
    }
}
