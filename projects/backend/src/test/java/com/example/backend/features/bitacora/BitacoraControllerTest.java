package com.example.backend.features.bitacora;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.backend.features.bitacora.service.BitacoraPurgaJob;
import com.example.backend.support.PostgresRuntimeIntegrationTest;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Consulta y purga de la bitácora (HU-INICIAL-Bitacora). */
@SpringBootTest
@AutoConfigureMockMvc
class BitacoraControllerTest extends PostgresRuntimeIntegrationTest {

    private static final String ADMIN = "{\"email\":\"admin@test.local\",\"password\":\"TestAdmin12345\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BitacoraPurgaJob purga;

    private Cookie admin;
    private Cookie refreshAdmin;
    private String modulo;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void preparar() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(ADMIN)).andExpect(status().isOk()).andReturn();
        admin = login.getResponse().getCookie("accessToken");
        refreshAdmin = login.getResponse().getCookie("refreshToken");
        // Módulo único por test: aísla sus eventos de los que generan otros.
        modulo = "t" + UUID.randomUUID().toString().substring(0, 8);
        evento("PRIMERO", "EXITO", null, "2026-01-10T10:00:00Z");
        evento("SEGUNDO", "FALLO", actor, "2026-02-10T10:00:00Z");
        evento("TERCERO", "EXITO", actor, "2026-03-10T10:00:00Z");
    }

    private void evento(String accion, String resultado, UUID usuario, String fecha) {
        dueno().update("INSERT INTO authentication.bitacora (codigo_bitacora, usuario_codigo, accion, modulo, "
                + "resultado, detalle, created_at) VALUES (gen_random_uuid(), ?, ?, ?, ?, '{\"k\":\"v\"}'::jsonb, "
                + "?::timestamptz)", usuario, accion, modulo, resultado, fecha);
    }

    private MockHttpServletRequestBuilder consulta() {
        return get("/api/v1/bitacora").param("filter[modulo]", modulo).cookie(admin);
    }

    private List<String> acciones(MockHttpServletRequestBuilder request) throws Exception {
        return JsonPath.read(mvc.perform(request).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(), "$.data[*].accion");
    }

    @Test
    void listar_devuelveDelMasRecienteAlMasAntiguoConElSobreSinIdInterno() throws Exception {
        mvc.perform(consulta())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].accion").value("TERCERO"))
                .andExpect(jsonPath("$.data[2].accion").value("PRIMERO"))
                .andExpect(jsonPath("$.data[0].codigo").exists())
                .andExpect(jsonPath("$.data[0].fecha").exists())
                .andExpect(jsonPath("$.data[0].detalle.k").value("v"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.meta.totalItems").value(3));
    }

    @Test
    void listar_paginaConMaximoDe100() throws Exception {
        mvc.perform(consulta().param("page", "2").param("pageSize", "2"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accion").value("PRIMERO"))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.meta.page").value(2));
        mvc.perform(consulta().param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void listar_aplicaCadaFiltroEIncluyeLosExtremosDelRango() throws Exception {
        assertThat(acciones(consulta().param("filter[accion]", "SEGUNDO"))).containsExactly("SEGUNDO");
        assertThat(acciones(consulta().param("filter[resultado]", "EXITO"))).containsExactly("TERCERO", "PRIMERO");
        assertThat(acciones(consulta().param("filter[usuarioCodigo]", actor.toString())))
                .containsExactly("TERCERO", "SEGUNDO");
        assertThat(acciones(consulta().param("desde", "2026-02-10T10:00:00Z").param("hasta", "2026-03-10T10:00:00Z")))
                .containsExactly("TERCERO", "SEGUNDO");
        // Con zona horaria: 06:00-04:00 es 10:00Z.
        assertThat(acciones(consulta().param("hasta", "2026-01-10T06:00:00-04:00"))).containsExactly("PRIMERO");
        assertThat(acciones(consulta().param("filter[accion]", "x' OR '1'='1"))).isEmpty();
    }

    @Test
    void listar_conRangoInvertido_da400Catalogado() throws Exception {
        mvc.perform(consulta().param("desde", "2026-03-01T00:00:00Z").param("hasta", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_BIT_01"));
    }

    @Test
    void listar_conFiltrosMalFormados_da400() throws Exception {
        for (List<String> filtro : List.of(List.of("filter[resultado]", "QUIZAS"),
                List.of("filter[usuarioCodigo]", "abc"), List.of("desde", "ayer"))) {
            mvc.perform(consulta().param(filtro.get(0), filtro.get(1)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
    }

    @Test
    void listar_sinSesionDa401YConRolUsuarioDa403() throws Exception {
        mvc.perform(get("/api/v1/bitacora")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/bitacora").with(org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user(UUID.randomUUID().toString()).roles("USUARIO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
    }

    @Test
    void noExistenOperacionesDeEscritura() throws Exception {
        mvc.perform(delete("/api/v1/bitacora").with(csrf()).cookie(admin)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/v1/bitacora").with(csrf()).cookie(admin)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void losEventosDeSesionNoGuardanSecretos() throws Exception {
        MvcResult renovada = mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(refreshAdmin))
                .andExpect(status().isOk()).andReturn();
        // Reutilización y logout: generan sus propios eventos.
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(refreshAdmin));
        mvc.perform(post("/api/v1/auth/logout").with(csrf()).cookie(renovada.getResponse().getCookie("refreshToken")));
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test.local\",\"password\":\"ClaveEquivocada9\"}"));

        String eventos = dueno().queryForObject(
                "SELECT string_agg(coalesce(detalle::text, ''), ' ') FROM authentication.bitacora", String.class);

        assertThat(eventos).doesNotContain("TestAdmin12345", "ClaveEquivocada9", refreshAdmin.getValue(),
                renovada.getResponse().getCookie("refreshToken").getValue(), admin.getValue(), "$2a$");
        assertThat(eventos).doesNotContainPattern("[0-9a-f]{64}");
    }

    @Test
    void purga_borraSoloLoAnteriorALaRetencion() {
        evento("ANTIGUO", "EXITO", null, "2020-01-01T00:00:00Z");

        purga.purgar();

        assertThat(dueno().queryForList("SELECT accion FROM authentication.bitacora WHERE modulo = ?",
                String.class, modulo)).doesNotContain("ANTIGUO").contains("TERCERO");
    }
}
