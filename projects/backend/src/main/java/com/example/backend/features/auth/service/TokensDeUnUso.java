package com.example.backend.features.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import com.example.backend.common.util.Sha256;

/**
 * Tokens de un solo uso, con caducidad, y uno vigente por usuario. Cada
 * subclase usa su prefijo: un token de un tipo no sirve como otro.
 *
 * * En Redis queda solo el SHA-256 del token. Dos claves: token -> usuario y
 * * usuario -> hash vigente (para anular el anterior al emitir otro).
 * * Consultar NO consume: el token se gasta recién al confirmar el cambio.
 * * Los scripts tocan una clave que no va en KEYS (la del token anterior):
 * * válido con un solo Redis (el de este proyecto), no con Redis Cluster.
 * ! Si Redis falla, la excepción se propaga (falla cerrado).
 */
public abstract class TokensDeUnUso {

    private static final int BYTES = 32;
    private static final int LARGO_MAXIMO = 100;

    // * KEYS: usuario, token nuevo. ARGV: hash nuevo, código del usuario, TTL en ms, prefijo.
    private static final RedisScript<Long> EMITIR = new DefaultRedisScript<>("""
            local anterior = redis.call('GET', KEYS[1])
            if anterior then redis.call('DEL', ARGV[4] .. anterior) end
            redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3])
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[3])
            return 1
            """, Long.class);

    // * KEYS: token, usuario. ARGV: código del usuario, hash. Devuelve 1 a un único llamador.
    private static final RedisScript<Long> CONSUMIR = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[1])
            if redis.call('GET', KEYS[2]) == ARGV[2] then redis.call('DEL', KEYS[2]) end
            return 1
            """, Long.class);

    // * KEYS: usuario. ARGV: prefijo.
    private static final RedisScript<Long> ANULAR = new DefaultRedisScript<>("""
            local vigente = redis.call('GET', KEYS[1])
            if vigente then redis.call('DEL', ARGV[1] .. vigente) end
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final String prefijo;
    private final String prefijoUsuario;
    private final Duration vigencia;
    private final SecureRandom azar = new SecureRandom();

    protected TokensDeUnUso(StringRedisTemplate redis, String prefijo, Duration vigencia) {
        this.redis = redis;
        this.prefijo = prefijo;
        this.prefijoUsuario = prefijo + "user:";
        this.vigencia = vigencia;
    }

    /** Emite un token nuevo y anula el anterior del mismo usuario (atómico). */
    public String emitir(UUID usuario) {
        byte[] bytes = new byte[BYTES];
        azar.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = Sha256.hex(token);
        redis.execute(EMITIR, List.of(prefijoUsuario + usuario, prefijo + hash),
                hash, usuario.toString(), String.valueOf(vigencia.toMillis()), prefijo);
        return token;
    }

    /** Usuario del token si sigue vigente. No lo consume. */
    public Optional<UUID> consultar(String token) {
        if (token == null || token.isBlank() || token.length() > LARGO_MAXIMO) {
            return Optional.empty();
        }
        return Optional.ofNullable(redis.opsForValue().get(prefijo + Sha256.hex(token))).map(UUID::fromString);
    }

    /** Anula el token vigente del usuario, si tiene uno (atómico). */
    public void anular(UUID usuario) {
        redis.execute(ANULAR, List.of(prefijoUsuario + usuario), prefijo);
    }

    /** Gasta el token. `false` si ya no es de ese usuario (usado, vencido o reemplazado). */
    public boolean consumir(String token, UUID usuario) {
        String hash = Sha256.hex(token);
        Long resultado = redis.execute(CONSUMIR, List.of(prefijo + hash, prefijoUsuario + usuario),
                usuario.toString(), hash);
        return Long.valueOf(1).equals(resultado);
    }
}
