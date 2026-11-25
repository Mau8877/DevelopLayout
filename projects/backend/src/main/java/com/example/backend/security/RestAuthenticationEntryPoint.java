package com.example.backend.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.backend.features.auth.AuthErrorCodes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde 401 con el sobre estándar cuando no hay credenciales válidas.
 *
 * * Se escribe el JSON a mano: corre a nivel de filtros, antes de que el
 * * @RestControllerAdvice pueda intervenir (mismo motivo que RateLimitFilter).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":"failed","data":null,"message":"No autenticado","timestamp":"%s","error":"%s"}"""
                .formatted(Instant.now().toString(), AuthErrorCodes.ERR_AUTH_07));
    }
}
