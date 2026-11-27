package com.example.backend.features.auth.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refresh tokens firmados: `{sesion}.{aleatorio}.{HMAC-SHA256}`.
 *
 * * La firma demuestra que el token lo emitió este servidor para esa sesión. Si
 * * es válida pero no es el token vigente, es uno ya rotado, por viejo que sea:
 * * no hace falta guardar historial.
 * * Para el cliente es un texto que no interpreta.
 * ! La clave es distinta de la de JWT. Cambiarla invalida todas las sesiones.
 * ! Se compara la firma como TEXTO y en tiempo constante: decodificar base64
 * ! aceptaría variantes del último carácter como la misma firma.
 */
@Component
public class RefreshTokens {

    private static final String ALGORITMO = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;
    private static final int LARGO_MAXIMO = 200;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();

    private final SecretKeySpec clave;

    public RefreshTokens(@Value("${app.auth.refresh-token-secret}") String secreto,
            @Value("${app.jwt.secret}") String secretoJwt) {
        byte[] bytes = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES || secreto.startsWith("CAMBIAME")) {
            throw new IllegalStateException("REFRESH_TOKEN_SECRET falta, es demasiado corta (mínimo 32 caracteres) "
                    + "o sigue con el valor de ejemplo.");
        }
        if (secreto.equals(secretoJwt)) {
            throw new IllegalStateException("REFRESH_TOKEN_SECRET tiene que ser distinta de JWT_SECRET.");
        }
        this.clave = new SecretKeySpec(bytes, ALGORITMO);
    }

    public String generar(UUID sesionCodigo) {
        byte[] aleatorio = new byte[32];
        RANDOM.nextBytes(aleatorio);
        byte[] sesion = ByteBuffer.allocate(16).putLong(sesionCodigo.getMostSignificantBits())
                .putLong(sesionCodigo.getLeastSignificantBits()).array();
        String contenido = CODIFICADOR.encodeToString(sesion) + "." + CODIFICADOR.encodeToString(aleatorio);
        return contenido + "." + firmar(contenido);
    }

    /** Código de sesión de un token con firma válida; vacío para cualquier otra cosa. */
    public Optional<UUID> sesionDe(String token) {
        if (token == null || token.length() > LARGO_MAXIMO) {
            return Optional.empty();
        }
        int corte = token.lastIndexOf('.');
        if (corte < 0) {
            return Optional.empty();
        }
        String contenido = token.substring(0, corte);
        boolean firmaValida = MessageDigest.isEqual(
                firmar(contenido).getBytes(StandardCharsets.UTF_8),
                token.substring(corte + 1).getBytes(StandardCharsets.UTF_8));
        if (!firmaValida) {
            return Optional.empty();
        }
        // * Con la firma válida, el contenido lo generó `generar`.
        ByteBuffer sesion = ByteBuffer.wrap(
                Base64.getUrlDecoder().decode(contenido.substring(0, contenido.indexOf('.'))));
        return Optional.of(new UUID(sesion.getLong(), sesion.getLong()));
    }

    private String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(clave);
            return CODIFICADOR.encodeToString(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el refresh token", e);
        }
    }
}
