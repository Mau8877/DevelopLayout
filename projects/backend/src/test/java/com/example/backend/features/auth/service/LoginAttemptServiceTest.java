package com.example.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

@SpringBootTest
class LoginAttemptServiceTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private LoginAttemptService service;

    @Autowired
    private StringRedisTemplate redis;

    private String correo() {
        return UUID.randomUUID() + "@test.local";
    }

    @Test
    void cuatroFallosNoBloquean_elQuintoSi() {
        String email = correo();
        for (int i = 0; i < 4; i++) {
            service.registrarFallo(email);
        }
        assertThat(service.estaBloqueado(email)).isFalse();

        service.registrarFallo(email);

        assertThat(service.estaBloqueado(email)).isTrue();
    }

    @Test
    void elCorreoSeNormaliza() {
        String email = correo();
        for (int i = 0; i < 5; i++) {
            service.registrarFallo(email.toUpperCase());
        }
        assertThat(service.estaBloqueado(" " + email + " ")).isTrue();
    }

    @Test
    void reiniciar_liberaElBloqueo() {
        String email = correo();
        for (int i = 0; i < 5; i++) {
            service.registrarFallo(email);
        }
        service.reiniciar(email);

        assertThat(service.estaBloqueado(email)).isFalse();
    }

    @Test
    void laClaveTieneTtlYNoContieneElCorreo() {
        String email = correo();
        service.registrarFallo(email);

        Set<String> claves = redis.keys("auth:login:fail:*");
        assertThat(claves).isNotEmpty().noneMatch(k -> k.contains(email));
        Long ttl = redis.getExpire(claves.iterator().next());
        assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(15).toSeconds());
    }
}
