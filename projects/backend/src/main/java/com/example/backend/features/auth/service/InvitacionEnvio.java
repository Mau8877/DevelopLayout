package com.example.backend.features.auth.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Emite una invitación nueva (anula la anterior) y la envía.
 *
 * ! Se llama sin transacción abierta: ni Redis ni el SMTP corren con una
 * ! conexión tomada.
 */
@Service
@RequiredArgsConstructor
public class InvitacionEnvio {

    private static final Logger LOG = LoggerFactory.getLogger(InvitacionEnvio.class);

    private final InvitacionTokenService tokens;
    private final CorreoCuentaService correo;

    /** @return `false` si no se pudo enviar; el fallo no se propaga. */
    public boolean enviar(UUID usuario, String email) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            try {
                correo.enviarInvitacion(email, tokens.emitir(usuario));
                LOG.info("Invitación enviada usuarioCodigo={}", usuario);
                return true;
            } catch (RuntimeException e) {
                // ! Sin el mensaje de la excepción: el de SMTP puede incluir el correo.
                LOG.error("No se pudo enviar la invitación usuarioCodigo={} causa={}", usuario,
                        e.getClass().getSimpleName());
                return false;
            }
        }
    }
}
