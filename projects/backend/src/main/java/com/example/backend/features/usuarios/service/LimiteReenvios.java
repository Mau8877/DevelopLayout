package com.example.backend.features.usuarios.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Cuántas veces por hora se puede reenviar la invitación de una cuenta.
 *
 * ! Si Redis falla, la excepción se propaga y no se envía (falla cerrado).
 */
@Service
public class LimiteReenvios {

    private static final Duration VENTANA = Duration.ofHours(1);

    private static final RedisScript<Long> CONTAR = new DefaultRedisScript<>("""
            local total = redis.call('INCR', KEYS[1])
            if total == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return total
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int maxPorHora;

    public LimiteReenvios(StringRedisTemplate redis,
            @Value("${app.auth.invite-resend-max-per-hour:3}") int maxPorHora) {
        if (maxPorHora < 1) {
            throw new IllegalStateException("app.auth.invite-resend-max-per-hour debe ser 1 o más");
        }
        this.redis = redis;
        this.maxPorHora = maxPorHora;
    }

    /** `true` si todavía se puede reenviar a esta cuenta (y cuenta el intento). */
    public boolean reservar(UUID usuario) {
        Long total = redis.execute(CONTAR, List.of("usuarios:reenvio:" + usuario),
                String.valueOf(VENTANA.toMillis()));
        if (total == null) {
            throw new IllegalStateException("Redis no devolvió el contador de reenvíos");
        }
        return total <= maxPorHora;
    }
}
