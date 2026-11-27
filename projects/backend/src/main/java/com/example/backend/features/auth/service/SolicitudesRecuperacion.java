package com.example.backend.features.auth.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.example.backend.common.util.Sha256;

/**
 * Límite de solicitudes de recuperación por correo (ventana fija de una hora).
 *
 * * Cuenta ANTES de saber si la cuenta existe: se comporta igual para cualquier
 * * correo. Reservar en un script evita que solicitudes simultáneas lo salten.
 */
@Service
public class SolicitudesRecuperacion {

    private static final Duration VENTANA = Duration.ofHours(1);

    private static final RedisScript<Long> CONTAR = new DefaultRedisScript<>("""
            local total = redis.call('INCR', KEYS[1])
            if total == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return total
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int maxPorHora;

    public SolicitudesRecuperacion(StringRedisTemplate redis,
            @Value("${app.auth.forgot-max-per-hour:3}") int maxPorHora) {
        if (maxPorHora < 1) {
            throw new IllegalStateException("app.auth.forgot-max-per-hour debe ser 1 o más");
        }
        this.redis = redis;
        this.maxPorHora = maxPorHora;
    }

    /** `true` si este correo todavía puede recibir un enlace. El correo llega ya normalizado. */
    public boolean reservar(String email) {
        Long total = redis.execute(CONTAR, List.of("auth:forgot:" + Sha256.hex(email)),
                String.valueOf(VENTANA.toMillis()));
        if (total == null) {
            throw new IllegalStateException("Redis no devolvió el contador de solicitudes");
        }
        return total <= maxPorHora;
    }
}
