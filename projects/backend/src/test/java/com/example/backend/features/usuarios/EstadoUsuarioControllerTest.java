package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.example.backend.features.auth.service.InvitacionTokenService;
import com.example.backend.features.auth.service.PasswordResetTokenService;

import jakarta.servlet.http.Cookie;

/** Desactivar y reactivar (CA12 a CA15, CA17 a CA19). */
class EstadoUsuarioControllerTest extends InvitacionesIntegrationTest {

    @Autowired
    private InvitacionTokenService invitaciones;

    @Autowired
    private PasswordResetTokenService recuperaciones;

    private static String ruta(UUID codigo, String accion) {
        return "/api/v1/users/" + codigo + "/" + accion;
    }

    private ResultActions pedir(Cuenta actor, UUID codigo, String accion) throws Exception {
        return mvc.perform(como(actor, post(ruta(codigo, accion))));
    }

    @Test
    void desactivarCortaElAccesoYReactivarLoDevuelveSinRevivirNada() throws Exception {
        String password = PASSWORD;
        Cuenta cuenta = crearCuenta("victima", "USUARIO");
        var inicio = login(cuenta.email(), password).andExpect(status().isOk()).andReturn().getResponse();
        Cookie access = inicio.getCookie("accessToken");
        Cookie refresh = inicio.getCookie("refreshToken");
        String recuperacion = recuperaciones.emitir(cuenta.codigo());
        String invitacion = invitaciones.emitir(cuenta.codigo());

        pedir(admin, cuenta.codigo(), "deactivate")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("INACTIVO"))
                .andExpect(jsonPath("$.data.roles[0]").value("USUARIO"))
                .andExpect(jsonPath("$.data.id").doesNotExist());

        mvc.perform(get("/api/v1/auth/me").cookie(access)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(refresh))
                .andExpect(status().isUnauthorized());
        login(cuenta.email(), password).andExpect(status().isForbidden());
        assertThat(recuperaciones.consultar(recuperacion)).isEmpty();
        assertThat(invitaciones.consultar(invitacion)).isEmpty();
        String enlaceTardio = recuperaciones.emitir(cuenta.codigo());

        pedir(admin, cuenta.codigo(), "reactivate")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"));

        mvc.perform(get("/api/v1/auth/me").cookie(access)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(refresh))
                .andExpect(status().isUnauthorized());
        assertThat(recuperaciones.consultar(enlaceTardio)).isEmpty();
        login(cuenta.email(), password).andExpect(status().isOk());
        assertThat(eventosSobre("USUARIOS_USUARIO_DESACTIVADO", cuenta.codigo())).isEqualTo(1);
        assertThat(eventosSobre("USUARIOS_USUARIO_REACTIVADO", cuenta.codigo())).isEqualTo(1);
    }

    @Test
    void repetirNoRegistraOtroEvento() throws Exception {
        Cuenta cuenta = crearCuenta("repetida", "USUARIO");

        pedir(admin, cuenta.codigo(), "reactivate").andExpect(status().isOk());
        pedir(admin, cuenta.codigo(), "deactivate").andExpect(status().isOk());
        pedir(admin, cuenta.codigo(), "deactivate").andExpect(status().isOk());

        assertThat(eventosSobre("USUARIOS_USUARIO_REACTIVADO", cuenta.codigo())).isZero();
        assertThat(eventosSobre("USUARIOS_USUARIO_DESACTIVADO", cuenta.codigo())).isEqualTo(1);
    }

    @Test
    void unaCuentaPendienteSigueSiendoloTrasDesactivarYReactivar() throws Exception {
        UUID codigo = codigoCreado(invitar("pend." + marca + "@test.local"));

        pedir(admin, codigo, "deactivate").andExpect(status().isOk());
        pedir(admin, codigo, "reactivate").andExpect(jsonPath("$.data.invitacionPendiente").value(true));

        assertThat(invitaciones.consultar(ultimoToken())).isEmpty();
    }

    @Test
    void nadieSeDesactivaASiMismo_yUnCodigoInexistenteEs404() throws Exception {
        pedir(admin, admin.codigo(), "deactivate")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_04"));
        assertThat(columna(admin.codigo(), "estado")).isEqualTo("ACTIVO");

        for (String accion : new String[] {"deactivate", "reactivate"}) {
            pedir(admin, UUID.randomUUID(), accion).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("ERR_US_01"));
        }
    }

    @Test
    void sinSesion401_sinRol403_sinCsrf403_ySinSesionVigente401() throws Exception {
        Cuenta objetivo = crearCuenta("objetivo", "USUARIO");
        Cuenta comun = conSesion(crearCuenta("comun", "USUARIO"));
        for (String accion : new String[] {"deactivate", "reactivate"}) {
            mvc.perform(post(ruta(objetivo.codigo(), accion)).with(csrf())).andExpect(status().isUnauthorized());
            pedir(comun, objetivo.codigo(), accion).andExpect(status().isForbidden());
            mvc.perform(post(ruta(objetivo.codigo(), accion))
                    .cookie(new Cookie("accessToken", admin.access()))).andExpect(status().isForbidden());
        }
        dueno().update("UPDATE authentication.sesion SET estado = 'REVOCADA' WHERE usuario_codigo = ?",
                admin.codigo());

        pedir(admin, objetivo.codigo(), "deactivate").andExpect(status().isUnauthorized());

        assertThat(columna(objetivo.codigo(), "estado")).isEqualTo("ACTIVO");
    }
}
