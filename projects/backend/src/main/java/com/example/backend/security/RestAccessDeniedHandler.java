package com.example.backend.security;

import com.example.backend.common.idioma.Mensajes;
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

    private final Mensajes mensajes;

    public RestAccessDeniedHandler(Mensajes mensajes) {
        this.mensajes = mensajes;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":"failed","data":null,"message":"%s","timestamp":"%s","error":"%s"}"""
                .formatted(mensajes.en(mensajes.idiomaDe(request), "common.error.accesoDenegado"), Instant.now().toString(), AuthErrorCodes.ERR_AUTH_08));
    }
}
