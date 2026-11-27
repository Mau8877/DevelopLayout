package com.example.backend.features.usuarios;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.backend.support.PostgresRuntimeIntegrationTest;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Apoyo de los tests de administración: cada test tiene su propio administrador con sesión. */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AdminUsuariosIntegrationTest extends PostgresRuntimeIntegrationTest {

    // * Contraseña inventada: las cuentas solo existen en la base descartable del test.
    protected static final String PASSWORD = "ClaveSegura123"; // gitleaks:allow

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private PasswordEncoder encoder;

    /** Parte única de los correos de cada test: la base la comparten todos. */
    protected String marca;

    protected Cuenta admin;

    /** `access` es null hasta que la cuenta inicia sesión. */
    protected record Cuenta(UUID codigo, String email, String access) {
    }

    @BeforeEach
    void prepararAdministrador() throws Exception {
        marca = "m" + UUID.randomUUID().toString().substring(0, 8);
        admin = conSesion(crearCuenta("admin", "SUPER_ADMIN"));
    }

    protected Cuenta crearCuenta(String nombre, String... roles) {
        UUID codigo = UUID.randomUUID();
        String email = nombre + "." + marca + "@test.local";
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (?, ?, ?, 'ACTIVO')", codigo, email, encoder.encode(PASSWORD));
        for (String rol : roles) {
            dueno().update("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                    + "SELECT gen_random_uuid(), u.id, r.id FROM authentication.usuario u, authentication.rol r "
                    + "WHERE u.codigo_usuario = ? AND r.nombre = ?", codigo, rol);
        }
        return new Cuenta(codigo, email, null);
    }

    protected Cuenta conSesion(Cuenta cuenta) throws Exception {
        String access = login(cuenta.email(), PASSWORD).andExpect(status().isOk()).andReturn()
                .getResponse().getCookie("accessToken").getValue();
        return new Cuenta(cuenta.codigo(), cuenta.email(), access);
    }

    protected ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    protected static MockHttpServletRequestBuilder como(Cuenta cuenta, MockHttpServletRequestBuilder request) {
        return request.with(csrf()).cookie(new Cookie("accessToken", cuenta.access()));
    }

    protected static MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder request, String json) {
        return request.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    protected static String leer(ResultActions respuesta, String ruta) throws Exception {
        return JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), ruta).toString();
    }

    protected String columna(UUID usuario, String columna) {
        return dueno().queryForObject("SELECT " + columna + "::text FROM authentication.usuario "
                + "WHERE codigo_usuario = ?", String.class, usuario);
    }

    protected int eventosSobre(String accion, UUID entidad) {
        Integer n = dueno().queryForObject("SELECT count(*) FROM authentication.bitacora "
                + "WHERE accion = ? AND entidad_codigo = ?", Integer.class, accion, entidad);
        return n == null ? 0 : n;
    }
}
