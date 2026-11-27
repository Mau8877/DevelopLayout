package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * Con un pool mucho más chico que la concurrencia, ningún flujo puede quedarse
 * esperando una segunda conexión mientras tiene tomada otra: eso bloqueaba la
 * app entera con 10 logins fallidos a la vez.
 */
@TestPropertySource(properties = {"spring.datasource.hikari.maximum-pool-size=3",
    "spring.datasource.hikari.connection-timeout=5000"})
class AuthPoolTest extends AuthSesionIntegrationTest {

    private static final int SIMULTANEAS = 24;

    private List<Integer> aLaVez(List<RequestBuilder> requests) throws Exception {
        List<Callable<Integer>> llamadas = new ArrayList<>();
        requests.forEach(r -> llamadas.add(() -> mvc.perform(r).andReturn().getResponse().getStatus()));
        List<Integer> estados = new ArrayList<>();
        try (var hilos = Executors.newFixedThreadPool(requests.size())) {
            for (var futuro : hilos.invokeAll(llamadas)) {
                estados.add(futuro.get());
            }
        }
        return estados;
    }

    private RequestBuilder login(String correo, String password) {
        return post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + correo + "\",\"password\":\"" + password + "\"}");
    }

    @Test
    void loginsFallidosSimultaneos_noAgotanElPool() throws Exception {
        List<RequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < SIMULTANEAS; i++) {
            requests.add(login("nadie-pool-" + i + "@test.local", "ClaveEquivocada1"));
        }

        assertThat(aLaVez(requests)).hasSize(SIMULTANEAS).containsOnly(401);
    }

    @Test
    void reutilizacionesSimultaneas_noAgotanElPool() throws Exception {
        List<RequestBuilder> reusos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            prepararUsuario();
            Tokens inicial = loginMobile();
            mvc.perform(mobile("refresh", inicial.refresh()));
            reusos.add(mobile("refresh", inicial.refresh()));
        }

        assertThat(aLaVez(reusos)).containsOnly(401);
    }
}
