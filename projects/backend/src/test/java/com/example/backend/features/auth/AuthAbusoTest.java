package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/** Límite de intentos de login frente a abuso (HU-INICIAL-Login CA03, CA13 y CA14). */
@TestPropertySource(properties = { "app.ratelimit.capacity=100000", "app.auth.max-failed-attempts-per-account=12" })
class AuthAbusoTest extends AuthSesionIntegrationTest {

    private static final String VICTIMA = "198.51.100.10";
    private static final String ATACANTE = "203.0.113.66";
    private static final String MALA = "ClaveEquivocada1";

    private int login(String ip, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf()).with(r -> {
            r.setRemoteAddr(ip);
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void intentosSimultaneos_soloSeEvaluanCincoContrasenas() throws Exception {
        for (int ronda = 0; ronda < 3; ronda++) {
            prepararUsuario();
            List<Callable<Integer>> intentos = Collections.nCopies(40, () -> login(ATACANTE, MALA));
            List<Integer> estados = new ArrayList<>();
            try (var hilos = Executors.newFixedThreadPool(40)) {
                for (var futuro : hilos.invokeAll(intentos)) {
                    estados.add(futuro.get());
                }
            }

            assertThat(estados).filteredOn(e -> e == 401).hasSize(5);
            assertThat(estados).filteredOn(e -> e == 423).hasSize(35);
        }
    }

    @Test
    void bloqueoDeUnOrigen_noDejaFueraAlUsuarioDesdeOtro() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(login(ATACANTE, MALA)).isEqualTo(401);
        }

        assertThat(login(ATACANTE, PASSWORD)).isEqualTo(423);
        assertThat(login(VICTIMA, PASSWORD)).isEqualTo(200);
    }

    @Test
    void topePorCuenta_frenaElAtaqueRepartidoYLoRegistra() throws Exception {
        for (int i = 0; i < 12; i++) {
            assertThat(login("203.0.113." + (i / 4), MALA)).isEqualTo(401);
        }

        assertThat(login("203.0.113.200", MALA)).isEqualTo(423);
        assertThat(login(VICTIMA, PASSWORD)).isEqualTo(423);
        assertThat(dueno().queryForObject("SELECT count(*) FROM authentication.bitacora "
                + "WHERE accion = 'AUTH_CUENTA_BLOQUEADA' AND detalle ->> 'alcance' = 'cuenta'", Integer.class))
                .isPositive();
    }

    @Test
    void loginCorrecto_reiniciaSuOrigenPeroNoBorraLosFallosAjenos() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(ATACANTE, MALA);
        }
        // La víctima: cuatro errores, acierta y vuelve a tener sus cinco intentos.
        for (int i = 0; i < 4; i++) {
            login(VICTIMA, MALA);
        }
        assertThat(login(VICTIMA, PASSWORD)).isEqualTo(200);
        assertThat(login(VICTIMA, MALA)).isEqualTo(401);
        assertThat(login(VICTIMA, PASSWORD)).isEqualTo(200);

        // En el total de la cuenta siguen los fallos: 5 + 4 + 1 = 10 de 12.
        assertThat(login("203.0.113.201", MALA)).isEqualTo(401);
        assertThat(login("203.0.113.202", MALA)).isEqualTo(401);
        assertThat(login("203.0.113.203", MALA)).isEqualTo(423);
    }
}
