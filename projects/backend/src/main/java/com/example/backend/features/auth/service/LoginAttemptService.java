package com.example.backend.features.auth.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.example.backend.common.util.Sha256;

/**
 * Límite de intentos de login con dos contadores en Redis: por correo e IP (el
 * bloqueo normal) y por cuenta sumando todas las IP (frena el ataque repartido).
 *
 * * Cada intento se RESERVA antes de comprobar la contraseña, en un script Lua
 * * (atómico): las peticiones simultáneas no pueden saltarse el límite. Si el
 * * login resulta correcto, la reserva se devuelve.
 * * Bloquear por correo+IP evita que un tercero deje fuera a otra persona; con
 * * suficientes IP todavía se puede alcanzar el tope por cuenta.
 * * Las claves usan SHA-256: ni el correo ni la IP quedan en claro.
 * ! Si Redis falla, la excepción se propaga a propósito (falla cerrado).
 */
@Service
public class LoginAttemptService {

    private static final String PREFIJO = "auth:login:fail:";

    // * KEYS: origen, cuenta, orígenes de la cuenta. ARGV: máximo por origen, máximo por cuenta, TTL en ms.
    // * El conjunto anota las claves de origen para poder borrarlas al restablecer la
    // * contraseña (llevan el hash de la IP, que no se puede adivinar).
    private static final RedisScript<Long> RESERVAR = new DefaultRedisScript<>("""
            if tonumber(redis.call('GET', KEYS[2]) or '0') >= tonumber(ARGV[2]) then return 2 end
            if tonumber(redis.call('GET', KEYS[1]) or '0') >= tonumber(ARGV[1]) then return 1 end
            for i = 1, 2 do
              redis.call('INCR', KEYS[i])
              redis.call('PEXPIRE', KEYS[i], ARGV[3])
            end
            redis.call('SADD', KEYS[3], KEYS[1])
            redis.call('PEXPIRE', KEYS[3], ARGV[3])
            return 0
            """, Long.class);

    // * KEYS: cuenta, orígenes de la cuenta. Sin SCAN ni KEYS.
    // ! Borra claves que no van en KEYS: válido con un solo Redis, no con Redis Cluster.
    private static final RedisScript<Long> REINICIAR = new DefaultRedisScript<>("""
            for _, origen in ipairs(redis.call('SMEMBERS', KEYS[2])) do
              redis.call('DEL', origen)
            end
            redis.call('DEL', KEYS[1], KEYS[2])
            return 0
            """, Long.class);

    // * Devuelve una reserva sin tocar el TTL ni crear claves que ya vencieron.
    private static final RedisScript<Long> DEVOLVER = new DefaultRedisScript<>("""
            for i = 1, #KEYS do
              if redis.call('EXISTS', KEYS[i]) == 1 and redis.call('DECR', KEYS[i]) <= 0 then
                redis.call('DEL', KEYS[i])
              end
            end
            return 0
            """, Long.class);

    public enum Reserva {
        PERMITIDO,
        BLOQUEADO_ORIGEN,
        BLOQUEADO_CUENTA
    }

    private final StringRedisTemplate redis;
    private final int maxPorOrigen;
    private final int maxPorCuenta;
    private final Duration duracionBloqueo;

    public LoginAttemptService(StringRedisTemplate redis,
            @Value("${app.auth.max-failed-attempts:5}") int maxPorOrigen,
            @Value("${app.auth.max-failed-attempts-per-account:100}") int maxPorCuenta,
            @Value("${app.auth.lock-duration:PT15M}") Duration duracionBloqueo) {
        if (maxPorOrigen < 1 || maxPorCuenta < maxPorOrigen) {
            throw new IllegalStateException(
                    "El máximo de intentos por cuenta no puede ser menor que el máximo por origen, y ambos son 1 o más");
        }
        this.redis = redis;
        this.maxPorOrigen = maxPorOrigen;
        this.maxPorCuenta = maxPorCuenta;
        this.duracionBloqueo = duracionBloqueo;
    }

    /** Pide permiso para comprobar una contraseña. Si se permite, ya cuenta como intento. */
    public Reserva reservar(String email, String ip) {
        Long resultado = redis.execute(RESERVAR,
                List.of(claveOrigen(email, ip), claveCuenta(email), claveOrigenes(email)),
                String.valueOf(maxPorOrigen), String.valueOf(maxPorCuenta),
                String.valueOf(duracionBloqueo.toMillis()));
        if (resultado == null) {
            throw new IllegalStateException("Redis no devolvió el resultado de la reserva");
        }
        return Reserva.values()[resultado.intValue()];
    }

    /** Login correcto: el origen vuelve a cero; en la cuenta solo se devuelve este intento. */
    public void confirmarExito(String email, String ip) {
        redis.delete(claveOrigen(email, ip));
        redis.execute(DEVOLVER, List.of(claveCuenta(email)));
    }

    /** El intento no fue un fallo de credenciales (ej. contraseña correcta de una cuenta inactiva). */
    public void liberar(String email, String ip) {
        redis.execute(DEVOLVER, List.of(claveOrigen(email, ip), claveCuenta(email)));
    }

    /** Levanta el bloqueo de la cuenta desde cualquier origen (contraseña restablecida). */
    public void reiniciar(String email) {
        redis.execute(REINICIAR, List.of(claveCuenta(email), claveOrigenes(email)));
    }

    private static String claveOrigenes(String email) {
        return claveCuenta(email) + ":origenes";
    }

    private static String claveCuenta(String email) {
        return PREFIJO + Sha256.hex(email.trim().toLowerCase(Locale.ROOT));
    }

    private static String claveOrigen(String email, String ip) {
        return claveCuenta(email) + ":" + Sha256.hex(String.valueOf(ip));
    }
}
