package com.example.backend.security;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Lista de sesiones revocadas (denylist) en Redis: `auth:revoked:{sid}`.
 *
 * * La fuente de verdad de las sesiones es la tabla `sesion`; esto solo
 * * permite cortar un access token vigente antes de que venza.
 */
@Component
public class SesionRevocadaRegistry {

    private static final String PREFIJO = "auth:revoked:";

    private final StringRedisTemplate redis;

    public SesionRevocadaRegistry(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** El TTL basta con la vida del access token: después ya no hay nada que cortar. */
    public void revocar(UUID sid, Duration ttl) {
        redis.opsForValue().set(PREFIJO + sid, "1", ttl);
    }

    public boolean estaRevocada(UUID sid) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIJO + sid));
    }
}
