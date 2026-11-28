package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.backend.features.auth.service.CambioPasswordIntentosService;

@Testcontainers(disabledWithoutDocker = true)
class CambioPasswordIntentosServiceTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory conexiones;
    private static StringRedisTemplate redis;

    @BeforeAll
    static void conectar() {
        conexiones = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        conexiones.afterPropertiesSet();
        redis = new StringRedisTemplate(conexiones);
    }

    @AfterAll
    static void cerrar() {
        conexiones.destroy();
    }

    private static CambioPasswordIntentosService servicio(Duration bloqueo) {
        return new CambioPasswordIntentosService(redis, 5, bloqueo);
    }

    private static long reservasPermitidas(CambioPasswordIntentosService servicio, UUID usuario, int intentos) {
        long permitidas = 0;
        for (int i = 0; i < intentos; i++) {
            if (servicio.reservar(usuario)) {
                permitidas++;
            }
        }
        return permitidas;
    }

    @Test
    void reservar_permiteCincoYReiniciarVuelveACero() {
        CambioPasswordIntentosService servicio = servicio(Duration.ofMinutes(15));
        UUID usuario = UUID.randomUUID();

        assertThat(reservasPermitidas(servicio, usuario, 6)).isEqualTo(5);
        assertThat(servicio.reservar(UUID.randomUUID())).isTrue();

        servicio.reiniciar(usuario);

        assertThat(servicio.reservar(usuario)).isTrue();
    }

    @Test
    void reservar_conCuarentaSimultaneas_permiteExactamenteCinco() throws Exception {
        CambioPasswordIntentosService servicio = servicio(Duration.ofMinutes(15));
        UUID usuario = UUID.randomUUID();
        CountDownLatch salida = new CountDownLatch(1);
        List<Callable<Boolean>> tareas = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            tareas.add(() -> {
                salida.await();
                return servicio.reservar(usuario);
            });
        }

        try (var hilos = Executors.newFixedThreadPool(40)) {
            var futuros = tareas.stream().map(hilos::submit).toList();
            salida.countDown();
            long permitidas = 0;
            for (var futuro : futuros) {
                if (futuro.get()) {
                    permitidas++;
                }
            }
            assertThat(permitidas).isEqualTo(5);
        }
    }

    @Test
    void reservar_sinCupo_noAlargaElBloqueo() {
        CambioPasswordIntentosService servicio = servicio(Duration.ofMinutes(15));
        UUID usuario = UUID.randomUUID();
        reservasPermitidas(servicio, usuario, 5);
        String clave = redis.keys("auth:cambio-password:fail:*").stream()
                .filter(c -> "5".equals(redis.opsForValue().get(c))).findFirst().orElseThrow();
        redis.expire(clave, Duration.ofSeconds(60));

        assertThat(servicio.reservar(usuario)).isFalse();

        assertThat(redis.getExpire(clave)).isBetween(1L, 60L);
        redis.delete(clave);
    }

    @Test
    void configuracionInvalida_noArranca() {
        assertThatThrownBy(() -> new CambioPasswordIntentosService(redis, 0, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CambioPasswordIntentosService(redis, 5, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class);
    }
}
