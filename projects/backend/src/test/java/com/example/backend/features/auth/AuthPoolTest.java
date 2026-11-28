package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.RequestBuilder;

import com.example.backend.features.auth.service.PasswordResetTokenService;

/**
 * Con un pool mucho más chico que la concurrencia, ningún flujo puede quedarse
 * esperando una segunda conexión mientras tiene tomada otra: eso bloqueaba la
 * app entera con 10 logins fallidos a la vez.
 */
@TestPropertySource(properties = {"spring.datasource.hikari.maximum-pool-size=3",
    "spring.datasource.hikari.connection-timeout=5000"})
class AuthPoolTest extends AuthSesionIntegrationTest {

    private static final int SIMULTANEAS = 24;

    @MockitoBean
    private JavaMailSender mail;

    @Autowired
    private PasswordResetTokenService tokens;

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

    @Test
    void recuperacionesSimultaneas_noAgotanElPool() throws Exception {
        List<RequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            prepararUsuario();
            UUID usuario = UUID.fromString(dueno().queryForObject(
                    "SELECT codigo_usuario::text FROM authentication.usuario WHERE email = ?", String.class, email));
            requests.add(publico("password/reset",
                    "{\"token\":\"" + tokens.emitir(usuario) + "\",\"password\":\"OtraClaveNueva456\"}")); // gitleaks:allow
            requests.add(publico("password/forgot", "{\"email\":\"" + email + "\"}"));
            requests.add(login(email, PASSWORD));
        }

        // * Un enlace pedido a la vez puede reemplazar al del restablecimiento (400) y
        // * un login puede llegar después del cambio (401). Lo que no puede haber es un 500.
        assertThat(aLaVez(requests)).hasSize(requests.size()).isSubsetOf(200, 400, 401);
    }

    @Test
    void cambiosDePasswordSimultaneos_noAgotanElPool() throws Exception {
        List<RequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            prepararUsuario();
            Tokens sesion = loginMobile();
            String ip = "10.77.0." + (i + 1);
            for (int j = 0; j < 3; j++) {
                // * Una actual incorrecta (bitácora en su transacción) y dos correctas a la vez.
                String actual = j == 0 ? "ClaveEquivocada1" : PASSWORD;
                requests.add(post("/api/v1/auth/password/change").header("X-Client-Type", "mobile")
                        .header("Authorization", "Bearer " + sesion.access())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"" + actual + "\",\"passwordNueva\":\"OtraClaveNueva" + j + "56\"}")
                        .with(r -> {
                            r.setRemoteAddr(ip);
                            return r;
                        }));
            }
        }

        // * Gana un cambio por cuenta; los demás llegan tarde (401) o fallan (400). Nunca 500.
        assertThat(aLaVez(requests)).hasSize(requests.size()).isSubsetOf(200, 400, 401);
    }

    private static RequestBuilder publico(String ruta, String cuerpo) {
        return post("/api/v1/auth/" + ruta).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }
}
