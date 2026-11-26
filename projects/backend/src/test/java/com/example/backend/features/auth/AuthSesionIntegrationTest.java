package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.backend.support.PostgresRuntimeIntegrationTest;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Apoyo común de los tests de renovación y cierre de sesión. */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AuthSesionIntegrationTest extends PostgresRuntimeIntegrationTest {

    protected static final String PASSWORD = "ClaveSegura123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private PasswordEncoder encoder;

    protected String email;

    protected record Tokens(String access, String refresh) {
    }

    @BeforeEach
    void prepararUsuario() {
        email = "s" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (?, ?, ?, 'ACTIVO')", UUID.randomUUID(), email, encoder.encode(PASSWORD));
        dueno().update("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                + "SELECT gen_random_uuid(), u.id, r.id FROM authentication.usuario u, authentication.rol r "
                + "WHERE u.email = ? AND r.nombre = 'USUARIO'", email);
    }

    protected Tokens loginWeb() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(credenciales()))
                .andExpect(status().isOk())
                .andReturn();
        return tokensDeCookies(res);
    }

    protected Tokens loginMobile() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login").header("X-Client-Type", "mobile")
                        .contentType(MediaType.APPLICATION_JSON).content(credenciales()))
                .andExpect(status().isOk())
                .andReturn();
        return tokensDelBody(res);
    }

    private String credenciales() {
        return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
    }

    protected static Tokens tokensDeCookies(MvcResult res) {
        return new Tokens(res.getResponse().getCookie("accessToken").getValue(),
                res.getResponse().getCookie("refreshToken").getValue());
    }

    protected static Tokens tokensDelBody(MvcResult res) throws Exception {
        String body = res.getResponse().getContentAsString();
        return new Tokens(JsonPath.read(body, "$.data.accessToken"), JsonPath.read(body, "$.data.refreshToken"));
    }

    protected static MockHttpServletRequestBuilder web(String accion, String refresh) {
        return post("/api/v1/auth/" + accion).with(csrf()).cookie(new Cookie("refreshToken", refresh));
    }

    protected static MockHttpServletRequestBuilder mobile(String accion, String refresh) {
        return post("/api/v1/auth/" + accion).header("X-Client-Type", "mobile")
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}");
    }

    protected void assertAccess(String access, int esperado) throws Exception {
        assertThat(mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("accessToken", access)))
                .andReturn().getResponse().getStatus()).isEqualTo(esperado);
    }

    protected String columnaSesion(String columna) {
        return dueno().queryForObject("SELECT s." + columna + "::text FROM authentication.sesion s "
                + "JOIN authentication.usuario u ON u.id = s.usuario_id WHERE u.email = ?", String.class, email);
    }

    protected int eventos(String accion) {
        Integer n = dueno().queryForObject("SELECT count(*) FROM authentication.bitacora b "
                + "JOIN authentication.usuario u ON u.codigo_usuario = b.usuario_codigo "
                + "WHERE u.email = ? AND b.accion = ?", Integer.class, email, accion);
        return n == null ? 0 : n;
    }
}
