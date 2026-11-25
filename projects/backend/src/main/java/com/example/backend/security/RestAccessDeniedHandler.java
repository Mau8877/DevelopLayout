package com.example.backend.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.backend.features.auth.AuthErrorCodes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde 403 con el sobre estándar (sin permiso, o CSRF inválido).
 *
 * * No distingue la causa hacia afuera: el mensaje es genérico a propósito.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":"failed","data":null,"message":"Acceso denegado","timestamp":"%s","error":"%s"}"""
                .formatted(Instant.now().toString(), AuthErrorCodes.ERR_AUTH_08));
    }
}
