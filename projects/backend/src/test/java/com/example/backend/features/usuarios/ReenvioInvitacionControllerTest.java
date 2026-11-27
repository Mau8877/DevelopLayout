package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.ResultActions;

import com.example.backend.features.auth.service.InvitacionTokenService;

import jakarta.servlet.http.Cookie;

/** Reenvío de la invitación (CA06, CA10, CA11, CA18, CA19). */
class ReenvioInvitacionControllerTest extends InvitacionesIntegrationTest {

    @Autowired
    private InvitacionTokenService tokens;

    private static String ruta(UUID codigo) {
        return "/api/v1/usuarios/" + codigo + "/reenviar-invitacion";
    }

    private ResultActions reenviar(UUID codigo) throws Exception {
        return mvc.perform(como(admin, post(ruta(codigo))));
    }

    private UUID cuentaPendiente() throws Exception {
        return codigoCreado(invitar("p." + marca + "@test.local").andExpect(status().isCreated()));
    }

    @Test
    void enviaUnEnlaceNuevoYAnulaElAnterior() throws Exception {
        UUID codigo = cuentaPendiente();
        String anterior = ultimoToken();

        reenviar(codigo).andExpect(status().isOk()).andExpect(jsonPath("$.data.invitacionEnviada").value(true));

        assertThat(bandeja).hasSize(2);
        assertThat(tokens.consultar(anterior)).isEmpty();
        assertThat(tokens.consultar(ultimoToken())).contains(codigo);
        assertThat(eventosSobre("USUARIOS_INVITACION_REENVIADA", codigo)).isEqualTo(1);
    }

    @Test
    void siElCorreoFallaLoDiceYQuedaRegistradoComoFallo() throws Exception {
        UUID codigo = cuentaPendiente();
        doThrow(new MailSendException("sin servidor")).when(mail).send(any(SimpleMailMessage.class));

        reenviar(codigo).andExpect(status().isOk()).andExpect(jsonPath("$.data.invitacionEnviada").value(false));

        assertThat(dueno().queryForObject("SELECT resultado FROM authentication.bitacora "
                + "WHERE entidad_codigo = ? AND accion = 'USUARIOS_INVITACION_REENVIADA'", String.class, codigo))
                .isEqualTo("FALLO");
    }

    @Test
    void cuentaInactivaOYaAceptada_es409SinCorreo() throws Exception {
        UUID codigo = cuentaPendiente();
        bandeja.clear();
        dueno().update("UPDATE authentication.usuario SET estado = 'INACTIVO' WHERE codigo_usuario = ?", codigo);
        reenviar(codigo).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_05"));

        Cuenta aceptada = crearCuenta("aceptada", "USUARIO");
        reenviar(aceptada.codigo()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_05"));

        assertThat(bandeja).isEmpty();
    }

    @Test
    void elCuartoReenvioEnLaHoraEs409() throws Exception {
        UUID codigo = cuentaPendiente();
        for (int i = 0; i < 3; i++) {
            reenviar(codigo).andExpect(status().isOk());
        }
        String vigente = ultimoToken();
        bandeja.clear();

        reenviar(codigo).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_US_06"));

        assertThat(bandeja).isEmpty();
        assertThat(tokens.consultar(vigente)).contains(codigo);
    }

    @Test
    void codigoInexistente404_yAccesoDenegado() throws Exception {
        reenviar(UUID.randomUUID()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ERR_US_01"));

        UUID codigo = cuentaPendiente();
        Cuenta comun = conSesion(crearCuenta("comun", "USUARIO"));
        mvc.perform(post(ruta(codigo)).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(como(comun, post(ruta(codigo)))).andExpect(status().isForbidden());
        mvc.perform(post(ruta(codigo)).cookie(new Cookie("accessToken", admin.access())))
                .andExpect(status().isForbidden());
    }
}
