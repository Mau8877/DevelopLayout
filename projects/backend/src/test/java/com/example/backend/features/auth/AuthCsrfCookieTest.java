package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.HttpCookie;
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
// * Sin Secure: el servidor de test es HTTP y el cliente de Java no devolvería las cookies.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.cookies.secure=false")
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

    // * Un navegador de verdad: la cookie XSRF-TOKEN tiene que sobrevivir a las
    // * requests autenticadas, o el siguiente refresh o logout recibe 403.
    @Test
    void trasUnaRequestAutenticada_laCookieCsrfSigueSirviendoParaRefreshYLogout() throws Exception {
        CookieManager cookies = new CookieManager();
        HttpClient browser = HttpClient.newBuilder().cookieHandler(cookies).build();

        assertThat(enviar(browser, cookies, "GET", "/csrf", null).statusCode()).isEqualTo(204);
        assertThat(enviar(browser, cookies, "POST", "/login",
                "{\"email\":\"admin@test.local\",\"password\":\"TestAdmin12345\"}").statusCode()).isEqualTo(200);

        HttpResponse<String> me = enviar(browser, cookies, "GET", "/me", null);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.headers().allValues("Set-Cookie")).noneMatch(c -> c.startsWith("XSRF-TOKEN=;"));

        assertThat(enviar(browser, cookies, "POST", "/refresh", null).statusCode()).isEqualTo(200);
        assertThat(enviar(browser, cookies, "GET", "/me", null).statusCode()).isEqualTo(200);
        assertThat(enviar(browser, cookies, "POST", "/logout", null).statusCode()).isEqualTo(200);
    }

    // * Contra el servidor real: sin la cabecera CSRF el cambio de contraseña no pasa.
    // ! No cambia la contraseña del administrador: la base la comparten otras pruebas.
    @Test
    void cambioDePassword_sinCabeceraCsrf_responde403_yConElla_llegaAlServicio() throws Exception {
        CookieManager cookies = new CookieManager();
        HttpClient browser = HttpClient.newBuilder().cookieHandler(cookies).build();
        enviar(browser, cookies, "GET", "/csrf", null);
        assertThat(enviar(browser, cookies, "POST", "/login",
                "{\"email\":\"admin@test.local\",\"password\":\"TestAdmin12345\"}").statusCode()).isEqualTo(200);
        String cuerpo = "{\"passwordActual\":\"ClaveEquivocada1\",\"passwordNueva\":\"OtraClaveNueva456\"}";

        HttpResponse<String> sinCsrf = browser.send(HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/password/change"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> conCsrf = enviar(browser, cookies, "POST", "/password/change", cuerpo);

        assertThat(sinCsrf.statusCode()).isEqualTo(403);
        assertThat(conCsrf.statusCode()).isEqualTo(400);
        assertThat(conCsrf.body()).contains("ERR_AUTH_12");
        enviar(browser, cookies, "POST", "/logout", null);
    }

    /** Como axios: copia la cookie XSRF-TOKEN vigente en el header X-XSRF-TOKEN. */
    private HttpResponse<String> enviar(HttpClient browser, CookieManager cookies, String metodo, String ruta,
            String json) throws Exception {
        HttpRequest.Builder request = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth" + ruta))
                .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json));
        if (json != null) {
            request.header("Content-Type", "application/json");
        }
        cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("XSRF-TOKEN") && !c.getValue().isEmpty())
                .map(HttpCookie::getValue)
                .findFirst()
                .ifPresent(token -> request.header("X-XSRF-TOKEN", token));
        return browser.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
