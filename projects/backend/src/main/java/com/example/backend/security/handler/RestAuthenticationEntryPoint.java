package com.example.backend.security.handler;

import com.example.backend.common.idioma.Mensajes;
import java.io.IOException;
import java.time.Instant;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.backend.features.auth.constant.AuthErrorCodes;

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

    private final Mensajes mensajes;

    public RestAuthenticationEntryPoint(Mensajes mensajes) {
        this.mensajes = mensajes;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":"failed","data":null,"message":"%s","timestamp":"%s","error":"%s"}"""
                .formatted(mensajes.en(mensajes.idiomaDe(request), "common.error.noAutenticado"), Instant.now().toString(), AuthErrorCodes.ERR_AUTH_07));
    }
}
