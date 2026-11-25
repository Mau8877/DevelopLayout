package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

/**
 * Verifica contra un servidor HTTP real que `GET /api/v1/auth/csrf` entrega la
 * cookie `XSRF-TOKEN` legible por JavaScript (double-submit).
 *
 * * Va con servidor real y no con MockMvc: MockMvc sustituye el repositorio de
 * * CSRF y no refleja lo que de verdad recibe el browser.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthCsrfCookieTest extends PostgresRuntimeIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void csrf_devuelve204YCookieXsrfLegiblePorJs() throws Exception {
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(204);
        String cookie = res.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElse(null);
        assertThat(cookie).as("cookie XSRF-TOKEN").isNotNull();
        assertThat(cookie).doesNotContain("HttpOnly");
        assertThat(cookie).contains("Path=/");
    }
}
