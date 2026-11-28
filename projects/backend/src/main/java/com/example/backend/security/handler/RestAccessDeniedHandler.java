package com.example.backend.security.handler;

import com.example.backend.common.idioma.Mensajes;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.backend.features.auth.constant.AuthErrorCodes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde 403 con el sobre estándar (sin permiso, o CSRF inválido).
 *
 * * No distingue la causa hacia afuera: el mensaje es genérico a propósito.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final Mensajes mensajes;

    public RestAccessDeniedHandler(Mensajes mensajes) {
        this.mensajes = mensajes;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        registrar(request);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":"failed","data":null,"message":"%s","timestamp":"%s","error":"%s"}"""
                .formatted(mensajes.en(mensajes.idiomaDe(request), "common.error.accesoDenegado"), Instant.now().toString(), AuthErrorCodes.ERR_AUTH_08));
    }

    // * Un acceso denegado es un evento de seguridad (A09). Sin la consulta de la URL
    // * ni datos de la persona: solo su código.
    private static void registrar(HttpServletRequest request) {
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        Object quien = actual != null && actual.getPrincipal() instanceof UUID codigo ? codigo : "sin-sesion";
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "security")) {
            LOG.warn("Acceso denegado metodo={} ruta={} usuarioCodigo={} errorCode={}", request.getMethod(),
                    request.getRequestURI(), quien, AuthErrorCodes.ERR_AUTH_08);
        }
    }
}
