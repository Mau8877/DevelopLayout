package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

/**
 * Intentos de saltarse un control escribiendo la petición de otra forma.
 *
 * * Contra un servidor real: MockMvc no decodifica las rutas ni aplica CSRF como
 * * el contenedor.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "app.ratelimit.login-capacity=3",
    "app.ratelimit.login-refill-per-minute=1",
    "app.ratelimit.capacity=40",
    "app.ratelimit.refill-per-minute=1",
})
class EvasionHttpTest extends PostgresRuntimeIntegrationTest {

    private static final String PASSWORD = "ClaveSegura123"; // gitleaks:allow

    @LocalServerPort
    private int puerto;

    @Autowired
    private PasswordEncoder encoder;

    private final HttpClient http = HttpClient.newHttpClient();

    // * Un solo método para los límites: comparten la cuota por IP de este contexto.
    @Test
    void escribirLaRutaConCodificacion_noEvitaNingunLimite() throws Exception {
        List<Integer> login = new ArrayList<>();
        for (String ruta : new String[] {"/api/v1/auth/login", "/api/v1/auth/lo%67in", "/api/v1/auth/logi%6E",
            "/api/v1/auth/%6Cogin", "/api/v1/auth/l%6Fgin"}) {
            login.add(postMobile(ruta, credenciales("nadie@test.local", PASSWORD)).statusCode());
        }
        // Tres de cupo para el login, se escriba como se escriba.
        assertThat(login).containsExactly(401, 401, 401, 429, 429);

        // El límite general tampoco se evita codificando el prefijo.
        List<Integer> generales = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            generales.add(enviar(HttpRequest.newBuilder(uri("/%61pi/v1/auth/me"))).statusCode());
        }
        assertThat(generales).contains(429);
    }

    @Test
    void unBearerInventado_noDesactivaElCsrfDeUnaPeticionConCookie() throws Exception {
        HttpResponse<String> respuesta = enviar(HttpRequest.newBuilder(uri("/api/v1/auth/logout"))
                .header("Authorization", "Bearer inventado")
                .header("Cookie", "refreshToken=cualquiera")
                .POST(HttpRequest.BodyPublishers.noBody()));

        assertThat(respuesta.statusCode()).isEqualTo(403);
    }

    @Test
    void unaContrasenaDeMasDe72Bytes_noRompeElLogin() throws Exception {
        String email = "bytes." + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (?, ?, ?, 'ACTIVO')", UUID.randomUUID(), email, encoder.encode(PASSWORD));

        // 40 letras con tilde: 40 caracteres, 80 bytes. Una cuenta real y una que no existe.
        for (String cuenta : new String[] {email, "nadie.bytes@test.local"}) {
            HttpResponse<String> respuesta = postMobile("/api/v1/auth/login", credenciales(cuenta, "ñ".repeat(40)));

            assertThat(respuesta.statusCode()).as(cuenta).isIn(400, 401, 429);
        }
    }

    private static String credenciales(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private HttpResponse<String> postMobile(String ruta, String body) throws Exception {
        return enviar(HttpRequest.newBuilder(uri(ruta))
                .header("X-Client-Type", "mobile")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private HttpResponse<String> enviar(HttpRequest.Builder peticion) throws Exception {
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
