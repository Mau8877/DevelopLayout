package com.example.backend.features.auth.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.example.backend.common.util.Sha256;

/**
 * Límite de intentos con la contraseña actual al cambiarla, por cuenta. Es
 * independiente de los contadores de login: agotarlo no impide iniciar sesión.
 *
 * * Cada intento se RESERVA antes de comparar, en un script Lua (atómico): las
 * * peticiones simultáneas no pueden probar más contraseñas que el límite.
 * * Cada reserva renueva el vencimiento; con el cupo agotado no suma ni alarga el
 * * bloqueo.
 * ! Si Redis falla, la excepción se propaga a propósito (falla cerrado).
 */
@Service
public class CambioPasswordIntentosService {

    private static final String PREFIJO = "auth:cambio-password:fail:";

    // * KEYS: cuenta. ARGV: máximo, TTL en ms. 1 = permitido, 0 = sin cupo.
    private static final RedisScript<Long> RESERVAR = new DefaultRedisScript<>("""
            if tonumber(redis.call('GET', KEYS[1]) or '0') >= tonumber(ARGV[1]) then return 0 end
            redis.call('INCR', KEYS[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int maximo;
    private final Duration bloqueo;

    public CambioPasswordIntentosService(StringRedisTemplate redis,
            @Value("${app.auth.password-change-max-attempts:5}") int maximo,
            @Value("${app.auth.password-change-lock:PT15M}") Duration bloqueo) {
        if (maximo < 1 || bloqueo.toMillis() < 1) {
            throw new IllegalStateException(
                    "app.auth.password-change-max-attempts debe ser 1 o más y password-change-lock, positivo");
        }
        this.redis = redis;
        this.maximo = maximo;
        this.bloqueo = bloqueo;
    }

    /** Pide permiso para comprobar la contraseña. Si se permite, ya cuenta como intento. */
    public boolean reservar(UUID usuario) {
        Long resultado = redis.execute(RESERVAR, List.of(clave(usuario)), String.valueOf(maximo),
                String.valueOf(bloqueo.toMillis()));
        if (resultado == null) {
            throw new IllegalStateException("Redis no devolvió el resultado de la reserva");
        }
        return resultado == 1;
    }

    /** La contraseña actual era correcta: la cuenta vuelve a cero. */
    public void reiniciar(UUID usuario) {
        redis.delete(clave(usuario));
    }

    private static String clave(UUID usuario) {
        return PREFIJO + Sha256.hex(usuario.toString());
    }
}
