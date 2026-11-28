package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Contrato HTTP de `POST /api/v1/auth/password/change`. */
class CambioPasswordControllerTest extends CambioPasswordIntegrationTest {

    private static String mensaje(MvcResult res) throws Exception {
        return JsonPath.read(res.getResponse().getContentAsString(), "$.message");
    }

    @Test
    void sinToken_responde401() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/password/change").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(PASSWORD, NUEVA))).andReturn();

        assertThat(estado(res)).isEqualTo(401);
        assertThat(error(res)).isEqualTo("ERR_AUTH_07");
    }

    @Test
    void webSinCsrf_responde403YNoCambiaNada() throws Exception {
        Tokens tokens = loginWeb();
        String hashAnterior = hashGuardado();

        MvcResult res = mvc.perform(post("/api/v1/auth/password/change")
                .cookie(new Cookie("accessToken", tokens.access()))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(PASSWORD, NUEVA))).andReturn();

        assertThat(estado(res)).isEqualTo(403);
        assertThat(hashGuardado()).isEqualTo(hashAnterior);
    }

    @Test
    void exitoWeb_borraLasDosCookies() throws Exception {
        MvcResult res = mvc.perform(cambioWeb(loginWeb(), cuerpo(PASSWORD, NUEVA))).andReturn();

        assertThat(estado(res)).isEqualTo(200);
        assertThat(res.getResponse().getContentAsString()).doesNotContain("\"data\":{");
        for (String nombre : new String[] {"accessToken", "refreshToken"}) {
            assertThat(res.getResponse().getCookie(nombre)).isNotNull();
            assertThat(res.getResponse().getCookie(nombre).getMaxAge()).isZero();
        }
    }

    @Test
    void exitoMobile_noEnviaCookies() throws Exception {
        MvcResult res = cambiar(loginMobile(), PASSWORD, NUEVA);

        assertThat(estado(res)).isEqualTo(200);
        assertThat(res.getResponse().getCookie("accessToken")).isNull();
        assertThat(res.getResponse().getCookie("refreshToken")).isNull();
    }

    @Test
    void errorWeb_noBorraLasCookies() throws Exception {
        MvcResult res = mvc.perform(cambioWeb(loginWeb(), cuerpo(OTRA, NUEVA))).andReturn();

        assertThat(estado(res)).isEqualTo(400);
        assertThat(res.getResponse().getCookie("accessToken")).isNull();
        assertThat(res.getResponse().getCookie("refreshToken")).isNull();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "es | La contraseña actual no es correcta",
        "en | The current password is not correct",
    })
    void actualIncorrecta_respondeEnElIdiomaPedido(String idioma, String esperado) throws Exception {
        MvcResult res = mvc.perform(cambioMobile(loginMobile(), cuerpo(OTRA, NUEVA))
                .header("Accept-Language", idioma)).andReturn();

        assertThat(mensaje(res)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "es | Demasiados intentos",
        "en | Too many attempts",
    })
    void bloqueado_respondeEnElIdiomaPedido(String idioma, String esperado) throws Exception {
        Tokens tokens = loginMobile();
        for (int i = 0; i < 5; i++) {
            cambiar(tokens, OTRA, NUEVA);
        }

        MvcResult res = mvc.perform(cambioMobile(tokens, cuerpo(PASSWORD, NUEVA))
                .header("Accept-Language", idioma)).andReturn();

        assertThat(estado(res)).isEqualTo(423);
        assertThat(mensaje(res)).startsWith(esperado);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"passwordNueva\":\"OtraClaveNueva456\"}",
        "{\"passwordActual\":\"ClaveSegura123\"}",
        "{\"passwordActual\":\"ClaveSegura123\",\"passwordNueva\":\"corta\"}",
        "{\"passwordActual\":\"ClaveSegura123\",\"passwordNueva\":\"ññññññññññññññññññññññññññññññññññññññññ\"}",
        "{\"passwordActual\":\"ClaveSegura123\",\"passwordNueva\":\"OtraClaveNueva456\",\"codigo\":\"x\"}",
    })
    void bodyInvalido_responde400SinGastarIntentos(String json) throws Exception {
        Tokens tokens = loginMobile();

        for (int i = 0; i < 6; i++) {
            MvcResult res = mvc.perform(cambioMobile(tokens, json)).andReturn();
            assertThat(estado(res)).isEqualTo(400);
            assertThat(error(res)).isEqualTo("ERR_SYS_01");
        }

        assertThat(estado(cambiar(tokens, PASSWORD, NUEVA))).isEqualTo(200);
    }

    @Test
    void superarElLimitePorIp_responde429() throws Exception {
        Tokens tokens = loginMobile();
        for (int i = 0; i < 10; i++) {
            cambiar(tokens, OTRA, NUEVA);
        }

        MvcResult res = cambiar(tokens, OTRA, NUEVA);

        assertThat(estado(res)).isEqualTo(429);
        assertThat(error(res)).isEqualTo("ERR_SYS_02");
    }
}
