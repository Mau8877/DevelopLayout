package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

/** Cierre de sesión (HU-INICIAL-Logout). */
class AuthLogoutTest extends AuthSesionIntegrationTest {

    @Test
    void logoutWeb_revocaBorraCookiesYCortaElAccessDeInmediato() throws Exception {
        Tokens tokens = loginWeb();
        assertAccess(tokens.access(), 200);

        MvcResult res = mvc.perform(web("logout", tokens.refresh()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andReturn();

        List<String> cookies = res.getResponse().getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(2).allSatisfy(c -> assertThat(c).contains("Max-Age=0"));
        assertThat(columnaSesion("estado")).isEqualTo("REVOCADA");
        assertThat(columnaSesion("motivo_revocacion")).isEqualTo("LOGOUT");
        assertAccess(tokens.access(), 401);
        mvc.perform(web("refresh", tokens.refresh())).andExpect(status().isUnauthorized());
        assertThat(eventos("AUTH_LOGOUT")).isEqualTo(1);
        assertThat(eventos("AUTH_REFRESH_REUSO")).isZero();
    }

    @Test
    void logout_repetidoOConTokenDesconocido_responde200SinRepetirElEvento() throws Exception {
        Tokens tokens = loginWeb();

        mvc.perform(web("logout", tokens.refresh())).andExpect(status().isOk());
        mvc.perform(web("logout", tokens.refresh())).andExpect(status().isOk());
        mvc.perform(web("logout", "no-existe")).andExpect(status().isOk());

        assertThat(eventos("AUTH_LOGOUT")).isEqualTo(1);
    }

    @Test
    void logoutMobile_usaElBodyEIgnoraLaCookie() throws Exception {
        Tokens tokens = loginMobile();

        mvc.perform(post("/api/v1/auth/logout").header("X-Client-Type", "mobile")
                        .cookie(new Cookie("refreshToken", tokens.refresh())))
                .andExpect(status().isOk());
        assertThat(columnaSesion("estado")).isEqualTo("ACTIVA");

        MvcResult res = mvc.perform(mobile("logout", tokens.refresh()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(columnaSesion("estado")).isEqualTo("REVOCADA");
        assertThat(res.getResponse().getHeaders("Set-Cookie")).isEmpty();
    }

    @Test
    void logoutWeb_sinTokenCsrf_da403YNoCierra() throws Exception {
        Tokens tokens = loginWeb();

        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refreshToken", tokens.refresh())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));

        assertThat(columnaSesion("estado")).isEqualTo("ACTIVA");
    }

    @Test
    void bitacoraDeLogout_noGuardaElRefreshToken() throws Exception {
        Tokens tokens = loginWeb();
        mvc.perform(web("logout", tokens.refresh()));

        Integer filtraciones = dueno().queryForObject(
                "SELECT count(*) FROM authentication.bitacora WHERE coalesce(detalle::text, '') LIKE ?",
                Integer.class, "%" + tokens.refresh() + "%");

        assertThat(filtraciones).isZero();
    }
}
