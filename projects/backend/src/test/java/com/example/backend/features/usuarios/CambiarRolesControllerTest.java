package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import jakarta.servlet.http.Cookie;

/** Cambio de roles (CA01 a CA05 y CA07 a CA10 de HU-INICIAL-Roles). */
class CambiarRolesControllerTest extends AdminUsuariosIntegrationTest {

    private static final String SOLO_ADMIN = "{\"roles\":[\"SUPER_ADMIN\"]}";
    private static final String SOLO_USUARIO = "{\"roles\":[\"USUARIO\"]}";
    private static final String AMBOS = "{\"roles\":[\"USUARIO\",\"SUPER_ADMIN\",\"USUARIO\"]}";

    private static String ruta(UUID codigo) {
        return "/api/v1/users/" + codigo + "/roles";
    }

    private ResultActions cambiar(Cuenta actor, UUID codigo, String cuerpo) throws Exception {
        return mvc.perform(como(actor, conJson(put(ruta(codigo)), cuerpo)));
    }

    private List<String> rolesActivos(UUID codigo) {
        return dueno().queryForList("SELECT r.nombre FROM authentication.usuario_rol ur "
                + "JOIN authentication.rol r ON r.id = ur.rol_id "
                + "JOIN authentication.usuario u ON u.id = ur.usuario_id "
                + "WHERE u.codigo_usuario = ? AND ur.is_active ORDER BY r.nombre", String.class, codigo);
    }

    private int filasDeRol(UUID codigo) {
        Integer n = dueno().queryForObject("SELECT count(*) FROM authentication.usuario_rol ur "
                + "JOIN authentication.usuario u ON u.id = ur.usuario_id WHERE u.codigo_usuario = ?",
                Integer.class, codigo);
        return n == null ? 0 : n;
    }

    @Test
    void dejaExactamenteEsosRoles_cierraLasSesionesYRigenAlVolverAEntrar() throws Exception {
        Cuenta persona = conSesion(crearCuenta("persona", "USUARIO"));

        cambiar(admin, persona.codigo(), SOLO_ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(1))
                .andExpect(jsonPath("$.data.roles[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        assertThat(rolesActivos(persona.codigo())).containsExactly("SUPER_ADMIN");
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("accessToken", persona.access())))
                .andExpect(status().isUnauthorized());
        login(persona.email(), PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usuario.roles[0]").value("SUPER_ADMIN"));
        assertThat(dueno().queryForObject("SELECT detalle::text FROM authentication.bitacora "
                + "WHERE entidad_codigo = ? AND accion = 'USUARIOS_ROLES_EDITADOS'", String.class, persona.codigo()))
                .contains("\"agregados\": [\"SUPER_ADMIN\"]").contains("\"quitados\": [\"USUARIO\"]")
                .doesNotContain(persona.email());
    }

    @Test
    void elMismoConjunto_noCierraSesionesNiRegistraNada() throws Exception {
        Cuenta persona = conSesion(crearCuenta("igual", "USUARIO"));

        cambiar(admin, persona.codigo(), "{\"roles\":[\"USUARIO\",\"USUARIO\"]}").andExpect(status().isOk());

        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("accessToken", persona.access())))
                .andExpect(status().isOk());
        assertThat(eventosSobre("USUARIOS_ROLES_EDITADOS", persona.codigo())).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"roles\":[]}",
        "{}",
        "{\"roles\":[\"JEFE\"]}",
        "{\"roles\":[\"USUARIO\",\"JEFE\"]}",
        "{\"roles\":[\"SUPER_ADMIN\"],\"estado\":\"INACTIVO\"}",
    })
    void datosInvalidos_son400SinCambios(String cuerpo) throws Exception {
        Cuenta persona = crearCuenta("invalida", "USUARIO");

        cambiar(admin, persona.codigo(), cuerpo).andExpect(status().isBadRequest());

        assertThat(rolesActivos(persona.codigo())).containsExactly("USUARIO");
    }

    @Test
    void volverADarUnRolQuitado_reutilizaLaMismaFila() throws Exception {
        Cuenta persona = crearCuenta("vaiven", "USUARIO");

        cambiar(admin, persona.codigo(), AMBOS).andExpect(status().isOk());
        cambiar(admin, persona.codigo(), SOLO_USUARIO).andExpect(status().isOk());
        cambiar(admin, persona.codigo(), AMBOS).andExpect(status().isOk());

        assertThat(rolesActivos(persona.codigo())).containsExactly("SUPER_ADMIN", "USUARIO");
        assertThat(filasDeRol(persona.codigo())).isEqualTo(2);
        assertThat(eventosSobre("USUARIOS_ROLES_EDITADOS", persona.codigo())).isEqualTo(3);
    }

    @Test
    void funcionaEnCuentasInactivasYPendientes() throws Exception {
        Cuenta persona = crearCuenta("dormida", "USUARIO");
        dueno().update("UPDATE authentication.usuario SET estado = 'INACTIVO', invitacion_pendiente = true "
                + "WHERE codigo_usuario = ?", persona.codigo());

        cambiar(admin, persona.codigo(), AMBOS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("INACTIVO"))
                .andExpect(jsonPath("$.data.invitacionPendiente").value(true));
    }

    @Test
    void nadieSeQuitaSuPropioRolDeAdministrador() throws Exception {
        cambiar(admin, admin.codigo(), SOLO_USUARIO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_07"));

        assertThat(rolesActivos(admin.codigo())).containsExactly("SUPER_ADMIN");
    }

    @Test
    void agregarseOtroRolCierraTambienLaSesionPropia() throws Exception {
        cambiar(admin, admin.codigo(), AMBOS).andExpect(status().isOk());

        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("accessToken", admin.access())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void codigoInexistente404_yAccesoDenegado() throws Exception {
        cambiar(admin, UUID.randomUUID(), SOLO_ADMIN).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_US_01"));

        Cuenta objetivo = crearCuenta("objetivo", "USUARIO");
        Cuenta comun = conSesion(crearCuenta("comun", "USUARIO"));
        mvc.perform(conJson(put(ruta(objetivo.codigo())), SOLO_ADMIN).with(csrf()))
                .andExpect(status().isUnauthorized());
        cambiar(comun, objetivo.codigo(), SOLO_ADMIN).andExpect(status().isForbidden());
        cambiar(comun, comun.codigo(), SOLO_ADMIN).andExpect(status().isForbidden());
        mvc.perform(conJson(put(ruta(objetivo.codigo())), SOLO_ADMIN)
                .cookie(new Cookie("accessToken", admin.access()))).andExpect(status().isForbidden());
        dueno().update("UPDATE authentication.sesion SET estado = 'REVOCADA' WHERE usuario_codigo = ?",
                admin.codigo());
        cambiar(admin, objetivo.codigo(), SOLO_ADMIN).andExpect(status().isUnauthorized());

        assertThat(rolesActivos(objetivo.codigo())).containsExactly("USUARIO");
        assertThat(rolesActivos(comun.codigo())).containsExactly("USUARIO");
    }
}
