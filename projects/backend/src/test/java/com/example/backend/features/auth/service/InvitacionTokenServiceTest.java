package com.example.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

@SpringBootTest
class InvitacionTokenServiceTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private InvitacionTokenService invitaciones;

    @Autowired
    private PasswordResetTokenService recuperaciones;

    @Autowired
    private StringRedisTemplate redis;

    private final UUID usuario = UUID.randomUUID();

    @Test
    void venceEnVeinticuatroHoras() {
        invitaciones.emitir(usuario);

        assertThat(redis.getExpire("auth:invite:user:" + usuario, TimeUnit.SECONDS)).isBetween(86_300L, 86_400L);
    }

    @Test
    void unTokenDeUnTipo_noSirveComoElOtro() {
        String invitacion = invitaciones.emitir(usuario);
        String recuperacion = recuperaciones.emitir(usuario);

        assertThat(recuperaciones.consultar(invitacion)).isEmpty();
        assertThat(recuperaciones.consumir(invitacion, usuario)).isFalse();
        assertThat(invitaciones.consultar(recuperacion)).isEmpty();
        assertThat(invitaciones.consumir(recuperacion, usuario)).isFalse();
    }

    @Test
    void anular_dejaElEnlaceSinEfectoYSoloElDeSuTipo() {
        String invitacion = invitaciones.emitir(usuario);
        String recuperacion = recuperaciones.emitir(usuario);

        invitaciones.anular(usuario);

        assertThat(invitaciones.consultar(invitacion)).isEmpty();
        assertThat(invitaciones.consumir(invitacion, usuario)).isFalse();
        assertThat(recuperaciones.consultar(recuperacion)).contains(usuario);
        assertThat(redis.hasKey("auth:invite:user:" + usuario)).isFalse();
    }

    @Test
    void anularSinToken_noFalla() {
        assertThatCode(() -> invitaciones.anular(UUID.randomUUID())).doesNotThrowAnyException();
    }
}
