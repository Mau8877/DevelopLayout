package com.example.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.backend.features.auth.service.LoginAttemptService.Reserva;
import com.example.backend.support.PostgresRuntimeIntegrationTest;

@SpringBootTest
class LoginAttemptServiceTest extends PostgresRuntimeIntegrationTest {

    private static final String IP = "198.51.100.1";

    @Autowired
    private LoginAttemptService service;

    @Autowired
    private StringRedisTemplate redis;

    private String correo() {
        return UUID.randomUUID() + "@test.local";
    }

    private void reservar(String email, String ip, int veces) {
        for (int i = 0; i < veces; i++) {
            assertThat(service.reservar(email, ip)).isEqualTo(Reserva.PERMITIDO);
        }
    }

    @Test
    void cincoIntentosSePermitenYElSextoSeBloqueaSoloParaEseOrigen() {
        String email = correo();
        reservar(email, IP, 5);

        assertThat(service.reservar(email, IP)).isEqualTo(Reserva.BLOQUEADO_ORIGEN);
        assertThat(service.reservar(email, "198.51.100.2")).isEqualTo(Reserva.PERMITIDO);
    }

    @Test
    void elCorreoSeNormaliza() {
        String email = correo();
        reservar(email.toUpperCase(), IP, 5);

        assertThat(service.reservar(" " + email + " ", IP)).isEqualTo(Reserva.BLOQUEADO_ORIGEN);
    }

    @Test
    void confirmarExito_reiniciaElOrigen() {
        String email = correo();
        reservar(email, IP, 5);

        service.confirmarExito(email, IP);

        reservar(email, IP, 5);
    }

    @Test
    void liberar_devuelveSoloEseIntento() {
        String email = correo();
        reservar(email, IP, 5);

        service.liberar(email, IP);

        reservar(email, IP, 1);
        assertThat(service.reservar(email, IP)).isEqualTo(Reserva.BLOQUEADO_ORIGEN);
    }

    @Test
    void lasClavesTienenTtlYNoContienenElCorreoNiLaIp() {
        String email = correo();
        redis.delete(redis.keys("auth:login:fail:*"));
        service.reservar(email, IP);

        Set<String> claves = redis.keys("auth:login:fail:*");
        assertThat(claves).hasSize(2).noneMatch(k -> k.contains(email) || k.contains(IP));
        claves.forEach(k -> assertThat(redis.getExpire(k)).isPositive()
                .isLessThanOrEqualTo(Duration.ofMinutes(15).toSeconds()));
    }

    @Test
    void topePorCuentaMenorQueElDeOrigen_noArranca() {
        assertThatThrownBy(() -> new LoginAttemptService(redis, 5, 4, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
    }
}
