package com.example.backend.features.auth.service;

import com.example.backend.common.idioma.Mensajes;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía los correos de la cuenta (recuperación e invitación), en texto plano.
 *
 * * El token va en el FRAGMENTO del enlace: el navegador no lo manda al servidor
 * * ni en `Referer`.
 * ! La dirección del enlace sale solo de la configuración, nunca de la petición
 * ! (una cabecera `Host` falsa serviría para robar el token).
 * ! No loguea nada: quien llama registra el fallo sin el mensaje de la
 * ! excepción, que puede traer el correo del destinatario.
 */
@Service
public class CorreoCuentaService {

    private static final Set<String> HOSTS_LOCALES = Set.of("localhost", "127.0.0.1");

    private final JavaMailSender mailSender;
    private final Mensajes mensajes;
    private final String remitente;
    private final String urlWeb;

    public CorreoCuentaService(JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String servidor,
            @Value("${app.mail.from:}") String remitente,
            @Value("${app.frontend-base-url:}") String urlWeb,
            Mensajes mensajes) {
        exigir(servidor, "SPRING_MAIL_HOST (servidor SMTP)");
        exigir(remitente, "MAIL_FROM (remitente de los correos)");
        exigir(urlWeb, "APP_FRONTEND_BASE_URL (dirección de la web)");
        this.mailSender = mailSender;
        this.mensajes = mensajes;
        this.remitente = remitente.trim();
        this.urlWeb = validarUrl(urlWeb.trim());
    }

    public void enviarRecuperacion(String destinatario, String token, Locale idioma) {
        enviarPlantilla(destinatario, "auth.correo.recuperacion", urlWeb + "/reset-password#token=" + token, idioma);
    }

    public void enviarInvitacion(String destinatario, String token, Locale idioma) {
        enviarPlantilla(destinatario, "auth.correo.invitacion", urlWeb + "/accept-invitation#token=" + token, idioma);
    }

    private void enviarPlantilla(String destinatario, String clave, String enlace, Locale idioma) {
        enviar(destinatario, mensajes.en(idioma, clave + ".asunto"), mensajes.en(idioma, clave + ".cuerpo", enlace));
    }

    private void enviar(String destinatario, String asunto, String texto) {
        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setFrom(remitente);
        correo.setTo(destinatario);
        correo.setSubject(asunto);
        correo.setText(texto);
        mailSender.send(correo);
    }

    private static void exigir(String valor, String variable) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta configurar " + variable);
        }
    }

    private static String validarUrl(String url) {
        String sinBarra = url.replaceAll("/+$", "");
        try {
            URI uri = new URI(sinBarra);
            boolean https = "https".equalsIgnoreCase(uri.getScheme());
            boolean localSinTls = "http".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null && HOSTS_LOCALES.contains(uri.getHost());
            if (uri.getHost() != null && (https || localSinTls) && uri.getRawQuery() == null
                    && uri.getRawFragment() == null && uri.getRawUserInfo() == null) {
                return sinBarra;
            }
        } catch (URISyntaxException e) {
            // Se informa abajo con el mismo mensaje.
        }
        throw new IllegalStateException(
                "APP_FRONTEND_BASE_URL debe ser una URL https (http solo para localhost), sin consulta ni fragmento");
    }
}
