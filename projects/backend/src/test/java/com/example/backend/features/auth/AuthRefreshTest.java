package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

import com.example.backend.security.jwt.SesionRevocadaRegistry;

import jakarta.servlet.http.Cookie;

/** Renovación de sesión (HU-INICIAL-Refresh): contrato HTTP, rotación y fallos. */
class AuthRefreshTest extends AuthSesionIntegrationTest {

    @MockitoSpyBean
    private SesionRevocadaRegistry revocadas;

    @Test
    void refreshWeb_rotaYDevuelveCookiesNuevasSinTokensEnElBody() throws Exception {
        Tokens inicial = loginWeb();
        String hashInicial = columnaSesion("refresh_hash");

        MvcResult res = mvc.perform(web("refresh", inicial.refresh()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.usuario.email").value(email))
                .andExpect(jsonPath("$.data.usuario.roles[0]").value("USUARIO"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        Tokens nuevos = tokensDeCookies(res);
        assertThat(nuevos.refresh()).isNotEqualTo(inicial.refresh());
        assertThat(columnaSesion("refresh_hash")).isNotEqualTo(hashInicial);
        assertAccess(nuevos.access(), 200);
        mvc.perform(web("refresh", nuevos.refresh())).andExpect(status().isOk());
    }

    @Test
    void refreshMobile_usaElBodyYDevuelveLosTokensEnElBody() throws Exception {
        Tokens inicial = loginMobile();

        MvcResult res = mvc.perform(mobile("refresh", inicial.refresh()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(tokensDelBody(res).refresh()).isNotEqualTo(inicial.refresh());
        assertThat(res.getResponse().getHeaders("Set-Cookie")).isEmpty();
    }

    @Test
    void refresh_cadaCanalIgnoraElTokenDelOtro() throws Exception {
        Tokens tokens = loginWeb();

        // Mobile está exento de CSRF: no puede autenticarse con la cookie.
        mvc.perform(post("/api/v1/auth/refresh").header("X-Client-Type", "mobile")
                        .cookie(new Cookie("refreshToken", tokens.refresh())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));
        // Web no lee el body.
        mvc.perform(post("/api/v1/auth/refresh")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + tokens.refresh() + "\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(columnaSesion("estado")).isEqualTo("ACTIVA");
    }

    @Test
    void refresh_conTokenDesconocidoOAusente_da401() throws Exception {
        mvc.perform(web("refresh", "no-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));
        mvc.perform(post("/api/v1/auth/refresh").header("X-Client-Type", "mobile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));
    }

    @Test
    void refresh_conTokenReutilizado_revocaLaSesionDeInmediatoYLoRegistra() throws Exception {
        Tokens inicial = loginWeb();
        Tokens nuevos = tokensDeCookies(mvc.perform(web("refresh", inicial.refresh())).andReturn());

        mvc.perform(web("refresh", inicial.refresh()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));

        // Confirmado en la base aunque la respuesta sea un error.
        assertThat(columnaSesion("estado")).isEqualTo("REVOCADA");
        assertThat(columnaSesion("motivo_revocacion")).isEqualTo("REUSO");
        assertAccess(nuevos.access(), 401);
        mvc.perform(web("refresh", nuevos.refresh())).andExpect(status().isUnauthorized());
        assertThat(eventos("AUTH_REFRESH_REUSO")).isEqualTo(1);

        mvc.perform(web("refresh", inicial.refresh())).andExpect(status().isUnauthorized());
        assertThat(eventos("AUTH_REFRESH_REUSO")).isEqualTo(1);
    }

    @Test
    void refresh_conUnTokenDeVariasRotacionesAtras_revocaLaSesion() throws Exception {
        Tokens victima = loginWeb();
        Tokens atacante = victima;
        for (int i = 0; i < 3; i++) {
            atacante = tokensDeCookies(mvc.perform(web("refresh", atacante.refresh()))
                    .andExpect(status().isOk()).andReturn());
        }

        mvc.perform(web("refresh", victima.refresh())).andExpect(status().isUnauthorized());

        assertThat(columnaSesion("estado")).isEqualTo("REVOCADA");
        assertThat(columnaSesion("motivo_revocacion")).isEqualTo("REUSO");
        assertThat(eventos("AUTH_REFRESH_REUSO")).isEqualTo(1);
        assertAccess(atacante.access(), 401);
        mvc.perform(web("refresh", atacante.refresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_conUnTokenAlterado_da401SinRevocarNada() throws Exception {
        Tokens tokens = loginWeb();
        String original = tokens.refresh();
        int medio = original.length() / 2;
        String alterado = original.substring(0, medio) + (original.charAt(medio) == 'A' ? 'B' : 'A')
                + original.substring(medio + 1);

        for (String falso : List.of(alterado, original + "x", "a.b.c", "..", original.replace('.', '-'))) {
            mvc.perform(web("refresh", falso))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));
        }

        assertThat(columnaSesion("estado")).isEqualTo("ACTIVA");
        assertThat(eventos("AUTH_REFRESH_REUSO")).isZero();
        mvc.perform(web("refresh", original)).andExpect(status().isOk());
    }

    @Test
    void refresh_simultaneoConElMismoToken_soloUnoObtieneTokens() throws Exception {
        Tokens tokens = loginMobile();
        Callable<Integer> canje = () -> mvc.perform(mobile("refresh", tokens.refresh()))
                .andReturn().getResponse().getStatus();

        List<Integer> estados = new ArrayList<>();
        try (var hilos = Executors.newFixedThreadPool(2)) {
            for (var futuro : hilos.invokeAll(List.of(canje, canje))) {
                estados.add(futuro.get());
            }
        }

        assertThat(estados).containsExactlyInAnyOrder(200, 401);
    }

    @Test
    void refresh_conVidaMaximaCumplida_marcaExpiradaYNoEmite() throws Exception {
        Tokens tokens = loginWeb();
        dueno().update("UPDATE authentication.sesion SET created_at = now() - interval '31 days' "
                + "WHERE codigo_sesion = ?::uuid", columnaSesion("codigo_sesion"));

        MvcResult res = mvc.perform(web("refresh", tokens.refresh()))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(res.getResponse().getHeaders("Set-Cookie")).isEmpty();
        assertThat(columnaSesion("estado")).isEqualTo("EXPIRADA");
    }

    @Test
    void refresh_conVencimientoPorInactividad_marcaExpirada() throws Exception {
        Tokens tokens = loginWeb();
        dueno().update("UPDATE authentication.sesion SET expira_en = now() - interval '1 minute' "
                + "WHERE codigo_sesion = ?::uuid", columnaSesion("codigo_sesion"));

        mvc.perform(web("refresh", tokens.refresh())).andExpect(status().isUnauthorized());

        assertThat(columnaSesion("estado")).isEqualTo("EXPIRADA");
    }

    @Test
    void refresh_cercaDelLimite_noExtiendeMasAllaDeLaVidaMaxima() throws Exception {
        Tokens tokens = loginWeb();
        dueno().update("UPDATE authentication.sesion SET created_at = now() - interval '29 days' "
                + "WHERE codigo_sesion = ?::uuid", columnaSesion("codigo_sesion"));

        mvc.perform(web("refresh", tokens.refresh())).andExpect(status().isOk());

        assertThat(dueno().queryForObject("SELECT expira_en <= created_at + interval '30 days' "
                + "FROM authentication.sesion WHERE codigo_sesion = ?::uuid", Boolean.class,
                columnaSesion("codigo_sesion"))).isTrue();
    }

    @Test
    void refresh_conUsuarioDesactivado_da401SinRotar() throws Exception {
        Tokens tokens = loginWeb();
        String hash = columnaSesion("refresh_hash");
        dueno().update("UPDATE authentication.usuario SET estado = 'INACTIVO' WHERE email = ?", email);

        mvc.perform(web("refresh", tokens.refresh()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));

        assertThat(columnaSesion("refresh_hash")).isEqualTo(hash);
    }

    @Test
    void refresh_sinRolesActivos_da401() throws Exception {
        Tokens tokens = loginWeb();
        dueno().update("UPDATE authentication.usuario_rol SET is_active = false WHERE usuario_id = "
                + "(SELECT id FROM authentication.usuario WHERE email = ?)", email);

        mvc.perform(web("refresh", tokens.refresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_conRedisCaido_da500GenericoSinTokensNiRotacion() throws Exception {
        Tokens tokens = loginWeb();
        String hash = columnaSesion("refresh_hash");
        doThrow(new RedisConnectionFailureException("detalle interno")).when(revocadas).estaRevocada(any());

        MvcResult res = mvc.perform(web("refresh", tokens.refresh()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("ERR_SYS_00"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado"))
                .andReturn();

        assertThat(res.getResponse().getHeaders("Set-Cookie")).isEmpty();
        assertThat(columnaSesion("refresh_hash")).isEqualTo(hash);
    }

    @Test
    void refreshWeb_sinTokenCsrf_da403() throws Exception {
        Tokens tokens = loginWeb();

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", tokens.refresh())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
    }
}
