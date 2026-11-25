package com.example.backend.features.auth.service;

import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.backend.common.util.Sha256;

/**
 * Cuenta intentos de login fallidos por correo y bloquea tras un máximo.
 *
 * * La clave usa el SHA-256 del correo normalizado: el correo no queda en
 * * claro en Redis.
 * ! Si Redis falla, la excepción se propaga a propósito (falla cerrado).
 */
@Service
public class LoginAttemptService {

    private static final String PREFIJO = "auth:login:fail:";

    private final StringRedisTemplate redis;
    private final int maxIntentos;
    private final Duration duracionBloqueo;

    public LoginAttemptService(StringRedisTemplate redis,
            @Value("${app.auth.max-failed-attempts:5}") int maxIntentos,
            @Value("${app.auth.lock-duration:PT15M}") Duration duracionBloqueo) {
        this.redis = redis;
        this.maxIntentos = maxIntentos;
        this.duracionBloqueo = duracionBloqueo;
    }

    public boolean estaBloqueado(String email) {
        String valor = redis.opsForValue().get(clave(email));
        return valor != null && Long.parseLong(valor) >= maxIntentos;
    }

    public void registrarFallo(String email) {
        String clave = clave(email);
        redis.opsForValue().increment(clave);
        redis.expire(clave, duracionBloqueo);
    }

    public void reiniciar(String email) {
        redis.delete(clave(email));
    }

    private static String clave(String email) {
        return PREFIJO + Sha256.hex(email.trim().toLowerCase(Locale.ROOT));
    }
}
