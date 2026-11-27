package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.backend.features.auth.service.SesionAdminService;

import jakarta.servlet.http.Cookie;

/** Administración de sesiones (HU-INICIAL-SesionesAdmin). */
class SesionAdminControllerTest extends AuthSesionIntegrationTest {

    @Autowired
    private SesionAdminService servicio;

    private String accessAdmin;
    private String codigoAdmin;

    @BeforeEach
    void iniciarAdmin() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test.local\",\"password\":\"TestAdmin12345\"}"))
                .andExpect(status().isOk()).andReturn();
        accessAdmin = login.getResponse().getCookie("accessToken").getValue();
        codigoAdmin = dueno().queryForObject(
                "SELECT codigo_usuario::text FROM authentication.usuario WHERE email = 'admin@test.local'", String.class);
    }

    private MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder request) {
        return request.with(csrf()).cookie(new Cookie("accessToken", accessAdmin));
    }

    private MockHttpServletRequestBuilder buscar(String json) {
        return post("/api/v1/admin/sesiones/buscar").contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String porCorreo(String correo) {
        return "{\"email\":\"" + correo + "\"}";
    }

    private String codigoUsuario() {
        return dueno().queryForObject("SELECT codigo_usuario::text FROM authentication.usuario WHERE email = ?",
                String.class, email);
    }

    @Test
    void buscar_devuelveLasSesionesVigentesConMetaYSinDatosInternos() throws Exception {
        loginWeb();
        loginMobile();

        MvcResult res = mvc.perform(comoAdmin(buscar(porCorreo(email.toUpperCase()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].tipoCliente").value("MOBILE"))
                .andExpect(jsonPath("$.data[0].usuarioCodigo").value(codigoUsuario()))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].usuarioId").doesNotExist())
                .andReturn();

        assertThat(res.getResponse().getContentAsString()).doesNotContainIgnoringCase("hash");
    }

    @Test
    void buscar_paginaYNoMuestraVencidas() throws Exception {
        loginWeb();
        loginWeb();
        loginWeb();
        dueno().update("UPDATE authentication.sesion SET expira_en = now() - interval '1 minute' WHERE id = "
                + "(SELECT max(s.id) FROM authentication.sesion s JOIN authentication.usuario u ON u.id = s.usuario_id "
                + "WHERE u.email = ?)", email);

        mvc.perform(comoAdmin(buscar("{\"email\":\"" + email + "\",\"page\":2,\"pageSize\":1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.meta.page").value(2));
    }

    @Test
    void buscar_correoInexistente_daListaVacia() throws Exception {
        mvc.perform(comoAdmin(buscar(porCorreo("nadie@test.local"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    @Test
    void buscar_conDatosInvalidos_da400() throws Exception {
        for (String json : List.of("{\"email\":\"no-es-correo\"}",
                "{\"email\":\"" + email + "\",\"pageSize\":101}",
                "{\"email\":\"" + email + "\",\"page\":0}",
                "{\"email\":\"" + email + "\",\"extra\":1}")) {
            mvc.perform(comoAdmin(buscar(json)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
    }

    @Test
    void cerrar_revocaDeInmediatoYRegistraActorYAfectado() throws Exception {
        Tokens victima = loginWeb();
        String sid = columnaSesion("codigo_sesion");

        mvc.perform(comoAdmin(delete("/api/v1/admin/sesiones/" + sid))).andExpect(status().isOk());

        assertThat(columnaSesion("estado")).isEqualTo("REVOCADA");
        assertThat(columnaSesion("motivo_revocacion")).isEqualTo("ADMIN");
        assertAccess(victima.access(), 401);
        mvc.perform(web("refresh", victima.refresh())).andExpect(status().isUnauthorized());
        assertThat(dueno().queryForObject("SELECT count(*) FROM authentication.bitacora WHERE accion = "
                + "'AUTH_SESION_CERRADA_ADMIN' AND entidad_codigo = ?::uuid AND usuario_codigo = ?::uuid "
                + "AND detalle ->> 'usuarioAfectado' = ?", Integer.class, sid, codigoAdmin, codigoUsuario())).isEqualTo(1);

        // Repetirlo no falla ni duplica el evento.
        mvc.perform(comoAdmin(delete("/api/v1/admin/sesiones/" + sid))).andExpect(status().isOk());
        assertThat(dueno().queryForObject("SELECT count(*) FROM authentication.bitacora WHERE entidad_codigo = ?::uuid "
                + "AND accion = 'AUTH_SESION_CERRADA_ADMIN'", Integer.class, sid)).isEqualTo(1);
    }

    @Test
    void cerrar_sesionInexistente_da404() throws Exception {
        mvc.perform(comoAdmin(delete("/api/v1/admin/sesiones/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_09"));
    }

    @Test
    void cerrarTodas_revocaCadaSesionDelUsuarioYSoloDeEse() throws Exception {
        Tokens una = loginWeb();
        Tokens otra = loginMobile();

        mvc.perform(comoAdmin(post("/api/v1/admin/usuarios/" + codigoUsuario() + "/sesiones/cerrar")))
                .andExpect(status().isOk());

        assertAccess(una.access(), 401);
        assertAccess(otra.access(), 401);
        assertAccess(accessAdmin, 200);
        assertThat(dueno().queryForObject("SELECT count(*) FROM authentication.bitacora WHERE accion = "
                + "'AUTH_SESION_CERRADA_ADMIN' AND detalle ->> 'usuarioAfectado' = ?", Integer.class, codigoUsuario()))
                .isEqualTo(2);
    }

    @Test
    void cerrar_simultaneoConUnRefresh_laSesionQuedaRevocada() throws Exception {
        for (int intento = 0; intento < 5; intento++) {
            Tokens tokens = loginMobile();
            UUID sid = UUID.fromString(dueno().queryForObject("SELECT s.codigo_sesion::text FROM "
                    + "authentication.sesion s JOIN authentication.usuario u ON u.id = s.usuario_id "
                    + "WHERE u.email = ? ORDER BY s.id DESC LIMIT 1", String.class, email));
            Callable<Integer> renovar = () -> mvc.perform(mobile("refresh", tokens.refresh()))
                    .andReturn().getResponse().getStatus();
            Callable<Integer> cerrar = () -> mvc.perform(comoAdmin(delete("/api/v1/admin/sesiones/" + sid)))
                    .andReturn().getResponse().getStatus();

            List<Integer> estados = new ArrayList<>();
            try (var hilos = Executors.newFixedThreadPool(2)) {
                for (var futuro : hilos.invokeAll(List.of(renovar, cerrar))) {
                    estados.add(futuro.get());
                }
            }

            assertThat(estados.get(1)).isEqualTo(200);
            assertThat(estados.get(0)).isIn(200, 401);
            assertThat(dueno().queryForObject("SELECT estado FROM authentication.sesion WHERE codigo_sesion = ?",
                    String.class, sid)).isEqualTo("REVOCADA");
        }
    }

    @Test
    void usuarioSinRolAdmin_recibe403EnTodasYNoCambiaNada() throws Exception {
        Tokens usuario = loginWeb();
        Cookie cookie = new Cookie("accessToken", usuario.access());
        String sid = columnaSesion("codigo_sesion");

        for (MockHttpServletRequestBuilder request : List.of(buscar(porCorreo("admin@test.local")),
                delete("/api/v1/admin/sesiones/" + sid),
                post("/api/v1/admin/usuarios/" + codigoAdmin + "/sesiones/cerrar"))) {
            mvc.perform(request.with(csrf()).cookie(cookie))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
        }

        assertThat(columnaSesion("estado")).isEqualTo("ACTIVA");
        assertAccess(accessAdmin, 200);
    }

    @Test
    void sinSesion_da401YSinCsrfDa403() throws Exception {
        mvc.perform(buscar(porCorreo(email)).with(csrf())).andExpect(status().isUnauthorized());

        Cookie cookie = new Cookie("accessToken", accessAdmin);
        mvc.perform(buscar(porCorreo(email)).cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/sesiones/" + UUID.randomUUID()).cookie(cookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void elServiceTambienDeniegaSinPasarPorHttp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
        try {
            assertThatThrownBy(() -> servicio.cerrarTodas(UUID.randomUUID()))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
