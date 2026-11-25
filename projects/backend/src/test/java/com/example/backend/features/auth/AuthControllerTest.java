package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

import jakarta.servlet.http.Cookie;

/**
 * Cubre el contrato HTTP de login: sobre `ApiResponse` completo, cookies,
 * canal mobile, CSRF, bloqueo y los eventos de bitácora.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends PostgresRuntimeIntegrationTest {

    private static final String PASSWORD = "ClaveSegura123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private StringRedisTemplate redis;

    private String email;

    @BeforeEach
    void prepararUsuario() {
        email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        crearUsuario(email, "ACTIVO");
    }

    private void crearUsuario(String correo, String estado) {
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (?, ?, ?, ?)", UUID.randomUUID(), correo, encoder.encode(PASSWORD), estado);
        dueno().update("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                + "SELECT gen_random_uuid(), u.id, r.id FROM authentication.usuario u, authentication.rol r "
                + "WHERE u.email = ? AND r.nombre = 'USUARIO'", correo);
    }

    private String body(String correo, String password) {
        return "{\"email\":\"" + correo + "\",\"password\":\"" + password + "\"}";
    }

    private MockHttpServletRequestBuilder loginWeb(String json) {
        return post("/api/v1/auth/login").with(org.springframework.security.test.web.servlet.request
                .SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private MockHttpServletRequestBuilder loginMobile(String json) {
        return post("/api/v1/auth/login").header("X-Client-Type", "mobile")
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private int eventos(String accion) {
        Integer n = dueno().queryForObject(
                "SELECT count(*) FROM authentication.bitacora WHERE accion = ?", Integer.class, accion);
        return n == null ? 0 : n;
    }

    // ---------------------------------------------------------------- éxito

    // * La cookie XSRF-TOKEN se verifica en AuthCsrfCookieTest, contra un servidor HTTP
    // * real: MockMvc simula el repositorio de CSRF y no sirve para probar la cookie.

    @Test
    void loginWeb_ok_seteaCookiesHttpOnlyStrictYNoDevuelveTokensEnElBody() throws Exception {
        int antes = eventos("AUTH_LOGIN_OK");

        MvcResult res = mvc.perform(loginWeb(body(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.usuario.email").value(email))
                .andExpect(jsonPath("$.data.usuario.roles[0]").value("USUARIO"))
                .andExpect(jsonPath("$.data.usuario.id").doesNotExist())
                .andExpect(jsonPath("$.data.usuario.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn();

        List<String> cookies = res.getResponse().getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(2);
        assertThat(cookies).anySatisfy(c -> assertThat(c)
                .startsWith("accessToken=").contains("HttpOnly", "SameSite=Strict", "Path=/api"));
        assertThat(cookies).anySatisfy(c -> assertThat(c)
                .startsWith("refreshToken=").contains("HttpOnly", "SameSite=Strict", "Path=/api/v1/auth"));
        assertThat(eventos("AUTH_LOGIN_OK")).isEqualTo(antes + 1);
        Integer sesiones = dueno().queryForObject(
                "SELECT count(*) FROM authentication.sesion s JOIN authentication.usuario u "
                        + "ON u.id = s.usuario_id WHERE u.email = ? AND s.estado = 'ACTIVA'", Integer.class, email);
        assertThat(sesiones).isEqualTo(1);
    }

    @Test
    void loginMobile_ok_devuelveTokensEnElBodyYNingunaCookie() throws Exception {
        MvcResult res = mvc.perform(loginMobile(body(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        assertThat(res.getResponse().getHeaders("Set-Cookie")).isEmpty();
    }

    // --------------------------------------------------------------- errores

    @Test
    void login_conPasswordMalaOCorreoInexistente_dan401GenericoIgual() throws Exception {
        int antes = eventos("AUTH_LOGIN_FALLO");

        String malaPassword = mvc.perform(loginWeb(body(email, "OtraClave12345")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"))
                .andReturn().getResponse().getContentAsString();
        String inexistente = mvc.perform(loginWeb(body("nadie@test.local", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"))
                .andReturn().getResponse().getContentAsString();

        assertThat(mensaje(malaPassword)).isEqualTo(mensaje(inexistente));
        assertThat(eventos("AUTH_LOGIN_FALLO")).isEqualTo(antes + 2);
        Integer conPassword = dueno().queryForObject(
                "SELECT count(*) FROM authentication.bitacora WHERE detalle::text ILIKE '%clave%'", Integer.class);
        assertThat(conPassword).isZero();
    }

    private static String mensaje(String json) {
        return json.replaceAll(".*\"message\":\"([^\"]*)\".*", "$1");
    }

    @Test
    void login_tras5Fallos_da423YRegistraElBloqueo() throws Exception {
        int antes = eventos("AUTH_CUENTA_BLOQUEADA");
        for (int i = 0; i < 5; i++) {
            mvc.perform(loginWeb(body(email, "OtraClave12345"))).andExpect(status().isUnauthorized());
        }

        mvc.perform(loginWeb(body(email, PASSWORD)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_02"));

        assertThat(eventos("AUTH_CUENTA_BLOQUEADA")).isEqualTo(antes + 1);
    }

    @Test
    void login_conUsuarioInactivo_da403() throws Exception {
        String inactivo = "inactivo" + UUID.randomUUID().toString().substring(0, 6) + "@test.local";
        crearUsuario(inactivo, "INACTIVO");

        mvc.perform(loginWeb(body(inactivo, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_03"));
    }

    @Test
    void login_conCamposInvalidos_da400() throws Exception {
        mvc.perform(loginWeb(body("no-es-correo", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mvc.perform(loginWeb(body(email, "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void login_conCampoExtra_da400() throws Exception {
        mvc.perform(loginWeb("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void loginWeb_sinTokenCsrf_da403() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
    }

    // ------------------------------------------------------------------ /me

    @Test
    void me_conCookieValida_devuelveElUsuario() throws Exception {
        MvcResult login = mvc.perform(loginWeb(body(email, PASSWORD))).andReturn();
        String access = login.getResponse().getCookie("accessToken").getValue();

        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("accessToken", access)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void me_sinSesion_da401ConElSobre() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
    }

    @Test
    void me_conBearerYHeaderMobile_funcionaPeroConCookieYHeaderMobileNo() throws Exception {
        String access = mvc.perform(loginMobile(body(email, PASSWORD))).andReturn().getResponse()
                .getContentAsString().replaceAll(".*\"accessToken\":\"([^\"]*)\".*", "$1");

        mvc.perform(get("/api/v1/auth/me").header("X-Client-Type", "mobile")
                .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("X-Client-Type", "mobile")
                .cookie(new Cookie("accessToken", access)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_conSesionRevocadaEnRedis_da401() throws Exception {
        MvcResult login = mvc.perform(loginWeb(body(email, PASSWORD))).andReturn();
        String access = login.getResponse().getCookie("accessToken").getValue();
        String sid = dueno().queryForObject("SELECT s.codigo_sesion::text FROM authentication.sesion s "
                + "JOIN authentication.usuario u ON u.id = s.usuario_id WHERE u.email = ?", String.class, email);
        redis.opsForValue().set("auth:revoked:" + sid, "1");

        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("accessToken", access)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointPrivado_conYSinToken() throws Exception {
        mvc.perform(get("/api/v1/otra-cosa")).andExpect(status().isUnauthorized());
    }
}
