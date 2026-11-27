package com.example.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

@SpringBootTest
class PasswordResetTokenServiceTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private PasswordResetTokenService tokens;

    @Autowired
    private StringRedisTemplate redis;

    private final UUID usuario = UUID.randomUUID();

    @Test
    void consultarNoConsume_yConsumirSirveUnaSolaVez() {
        String token = tokens.emitir(usuario);

        assertThat(tokens.consultar(token)).contains(usuario);
        assertThat(tokens.consultar(token)).contains(usuario);
        assertThat(tokens.consumir(token, usuario)).isTrue();
        assertThat(tokens.consumir(token, usuario)).isFalse();
        assertThat(tokens.consultar(token)).isEmpty();
    }

    @Test
    void emitirOtro_anulaElAnterior() {
        String viejo = tokens.emitir(usuario);
        String nuevo = tokens.emitir(usuario);

        assertThat(tokens.consultar(viejo)).isEmpty();
        assertThat(tokens.consumir(viejo, usuario)).isFalse();
        assertThat(tokens.consultar(nuevo)).contains(usuario);
    }

    @Test
    void consumirConOtroUsuario_noGastaElToken() {
        String token = tokens.emitir(usuario);

        assertThat(tokens.consumir(token, UUID.randomUUID())).isFalse();
        assertThat(tokens.consultar(token)).contains(usuario);
    }

    @Test
    void venceEnTreintaMinutos_yRedisNoGuardaElToken() {
        String token = tokens.emitir(usuario);

        var claves = redis.keys("auth:reset:*");
        assertThat(claves).noneMatch(c -> c.contains(token));
        assertThat(claves).allSatisfy(c -> assertThat(redis.opsForValue().get(c)).doesNotContain(token));
        assertThat(redis.getExpire("auth:reset:user:" + usuario, TimeUnit.SECONDS)).isBetween(1700L, 1800L);
        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
    }

    @Test
    void valoresQueNoSonUnToken_noSonValidos() {
        assertThat(tokens.consultar(null)).isEmpty();
        assertThat(tokens.consultar(" ")).isEmpty();
        assertThat(tokens.consultar("x".repeat(500))).isEmpty();
        assertThat(tokens.consultar("inventado")).isEmpty();
    }

    @Test
    void consumosSimultaneos_soloUnoGana() throws Exception {
        String token = tokens.emitir(usuario);
        List<Callable<Boolean>> llamadas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            llamadas.add(() -> tokens.consumir(token, usuario));
        }

        int ganadores = 0;
        try (var hilos = Executors.newFixedThreadPool(20)) {
            for (var futuro : hilos.invokeAll(llamadas)) {
                ganadores += futuro.get() ? 1 : 0;
            }
        }
        assertThat(ganadores).isEqualTo(1);
    }

    @Test
    void emisionesSimultaneas_dejanUnSoloTokenVigente() throws Exception {
        List<Callable<String>> llamadas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            llamadas.add(() -> tokens.emitir(usuario));
        }

        List<String> emitidos = new ArrayList<>();
        try (var hilos = Executors.newFixedThreadPool(20)) {
            for (var futuro : hilos.invokeAll(llamadas)) {
                emitidos.add(futuro.get());
            }
        }
        assertThat(emitidos).filteredOn(t -> tokens.consultar(t).isPresent()).hasSize(1);
    }
}
