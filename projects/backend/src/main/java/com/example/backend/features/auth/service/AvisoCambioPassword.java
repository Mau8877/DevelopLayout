package com.example.backend.features.auth.service;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

import com.example.backend.common.idioma.Mensajes;
import com.example.backend.config.RecuperacionEjecutorConfig;

/**
 * Avisa por correo que la contraseña de una cuenta cambió, venga del cambio con
 * sesión o de la recuperación.
 *
 * ! Se llama con el cambio ya confirmado y sin conexión tomada. Nunca falla hacia
 * ! quien llama: el cambio se mantiene aunque el correo no salga.
 * * Fuera de la petición: un SMTP lento no retrasa la respuesta. Sin cola
 * * persistente: un envío se puede perder.
 */
@Component
public class AvisoCambioPassword {

    private static final Logger LOG = LoggerFactory.getLogger(AvisoCambioPassword.class);

    private final CorreoCuentaService correo;
    private final Mensajes mensajes;
    private final Executor ejecutor;

    public AvisoCambioPassword(CorreoCuentaService correo, Mensajes mensajes,
            @Qualifier(RecuperacionEjecutorConfig.EJECUTOR) Executor ejecutor) {
        this.correo = correo;
        this.mensajes = mensajes;
        this.ejecutor = ejecutor;
    }

    public void enviar(UUID usuario, String email) {
        // * El idioma se captura acá: el hilo del ejecutor no tiene petición.
        Locale idioma = mensajes.idiomaActual();
        try {
            ejecutor.execute(() -> entregar(usuario, email, idioma));
        } catch (TaskRejectedException e) {
            LOG.warn("Aviso de cambio de contraseña descartado: cola de envío llena usuarioCodigo={}", usuario);
        }
    }

    // * Corre en un hilo del ejecutor: nada puede escapar de acá.
    private void entregar(UUID usuario, String email, Locale idioma) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            try {
                correo.enviarAvisoCambioPassword(email, idioma);
            } catch (RuntimeException e) {
                // ! Sin el mensaje de la excepción: el de SMTP puede incluir el correo.
                LOG.error("No se pudo enviar el aviso de cambio de contraseña usuarioCodigo={} causa={}", usuario,
                        e.getClass().getSimpleName());
            }
        }
    }
}
