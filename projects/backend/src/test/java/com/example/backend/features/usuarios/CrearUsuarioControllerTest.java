package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.ResultActions;

import com.example.backend.features.auth.service.InvitacionTokenService;

import jakarta.servlet.http.Cookie;

/** Alta con invitación (CA03 a CA06, CA09, CA17 a CA19, CA21). */
@ExtendWith(OutputCaptureExtension.class)
class CrearUsuarioControllerTest extends InvitacionesIntegrationTest {

    @Autowired
    private InvitacionTokenService tokens;

    private int cuentasCon(String email) {
        Integer n = dueno().queryForObject("SELECT count(*) FROM authentication.usuario WHERE lower(email) = lower(?)",
                Integer.class, email);
        return n == null ? 0 : n;
    }

    @Test
    void creaLaCuentaPendienteYEnviaLaInvitacion(CapturedOutput salida) throws Exception {
        String email = "nueva." + marca + "@test.local";

        ResultActions res = crear(admin, "{\"email\":\"Nueva." + marca + "@Test.Local\","
                + "\"roles\":[\"USUARIO\",\"SUPER_ADMIN\",\"USUARIO\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invitacionEnviada").value(true))
                .andExpect(jsonPath("$.data.usuario.email").value(email))
                .andExpect(jsonPath("$.data.usuario.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.data.usuario.invitacionPendiente").value(true))
                .andExpect(jsonPath("$.data.usuario.creadoEn").isNotEmpty())
                .andExpect(jsonPath("$.data.usuario.roles.length()").value(2));
        UUID codigo = codigoCreado(res);

        assertThat(bandeja).singleElement().satisfies(c -> assertThat(c.getTo()).containsExactly(email));
        String token = ultimoToken();
        assertThat(tokens.consultar(token)).contains(codigo);
        assertThat(res.andReturn().getResponse().getContentAsString())
                .doesNotContain(token).doesNotContainIgnoringCase("password").doesNotContainIgnoringCase("hash");
        assertThat(eventosSobre("USUARIOS_USUARIO_CREADO", codigo)).isEqualTo(1);
        assertThat(dueno().queryForObject("SELECT detalle::text FROM authentication.bitacora WHERE entidad_codigo = ?",
                String.class, codigo)).contains("SUPER_ADMIN").doesNotContain(email).doesNotContain(token);
        assertThat(salida.getAll()).doesNotContain(token).doesNotContain(email);
    }

    @Test
    void laCuentaPendienteFallaElLoginComoUnasCredencialesIncorrectas() throws Exception {
        String email = "pendiente." + marca + "@test.local";
        invitar(email).andExpect(status().isCreated());

        ResultActions pendiente = login(email, PASSWORD).andExpect(status().isUnauthorized());
        ResultActions inexistente = login("nadie." + marca + "@test.local", PASSWORD)
                .andExpect(status().isUnauthorized());

        assertThat(leer(pendiente, "$.error")).isEqualTo(leer(inexistente, "$.error")).isEqualTo("ERR_AUTH_01");
        assertThat(leer(pendiente, "$.message")).isEqualTo(leer(inexistente, "$.message"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"email\":\"no-es-correo\",\"roles\":[\"USUARIO\"]}",
        "{\"email\":\"valido@test.local\",\"roles\":[]}",
        "{\"email\":\"valido@test.local\"}",
        "{\"email\":\"valido@test.local\",\"roles\":[\"JEFE\"]}",
        "{\"email\":\"valido@test.local\",\"roles\":[\"USUARIO\"],\"estado\":\"INACTIVO\"}",
        "{\"email\":\"valido@test.local\",\"roles\":[\"USUARIO\"],\"password\":\"x\"}",
    })
    void datosInvalidos_son400YNoCreanNada(String cuerpo) throws Exception {
        crear(admin, cuerpo).andExpect(status().isBadRequest());

        assertThat(cuentasCon("valido@test.local")).isZero();
        assertThat(bandeja).isEmpty();
    }

    @Test
    void unCorreoYaUsadoEs409TambienSiLaCuentaEstaInactiva() throws Exception {
        Cuenta existente = crearCuenta("repetido", "USUARIO");
        dueno().update("UPDATE authentication.usuario SET estado = 'INACTIVO' WHERE codigo_usuario = ?",
                existente.codigo());

        invitar(existente.email().toUpperCase())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_02"));

        assertThat(cuentasCon(existente.email())).isEqualTo(1);
        assertThat(bandeja).isEmpty();
    }

    @Test
    void dosAltasSimultaneasDelMismoCorreo_unaSeCreaYLaOtraEs409() throws Exception {
        String email = "doble." + marca + "@test.local";
        List<Callable<Integer>> altas = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            altas.add(() -> invitar(email).andReturn().getResponse().getStatus());
        }

        List<Integer> estados = new ArrayList<>();
        try (var hilos = Executors.newFixedThreadPool(2)) {
            for (var futuro : hilos.invokeAll(altas)) {
                estados.add(futuro.get());
            }
        }

        assertThat(estados).containsExactlyInAnyOrder(201, 409);
        assertThat(cuentasCon(email)).isEqualTo(1);
    }

    @Test
    void siElCorreoFallaLaCuentaQuedaCreadaYSeAvisa(CapturedOutput salida) throws Exception {
        String email = "sincorreo." + marca + "@test.local";
        doThrow(new MailSendException("rechazado para " + email)).when(mail).send(any(SimpleMailMessage.class));

        invitar(email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invitacionEnviada").value(false));

        assertThat(cuentasCon(email)).isEqualTo(1);
        assertThat(salida.getAll()).contains("No se pudo enviar la invitación").doesNotContain(email);
    }

    @Test
    void sinSesion401_sinRol403_ySinCsrf403() throws Exception {
        Cuenta comun = conSesion(crearCuenta("comun", "USUARIO"));
        String cuerpo = "{\"email\":\"ajena." + marca + "@test.local\",\"roles\":[\"USUARIO\"]}";

        mvc.perform(conJson(post("/api/v1/users"), cuerpo).with(csrf())).andExpect(status().isUnauthorized());
        crear(comun, cuerpo).andExpect(status().isForbidden());
        mvc.perform(conJson(post("/api/v1/users"), cuerpo).cookie(new Cookie("accessToken", admin.access())))
                .andExpect(status().isForbidden());
        assertThat(cuentasCon("ajena." + marca + "@test.local")).isZero();
    }

    @Test
    void conLaSesionDelAdministradorCerradaEnLaBase_es401SinCrear() throws Exception {
        // * Solo en la base (sin la lista de revocadas): lo frena la guardia.
        dueno().update("UPDATE authentication.sesion SET estado = 'REVOCADA' WHERE usuario_codigo = ?",
                admin.codigo());

        invitar("tarde." + marca + "@test.local")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_04"));

        assertThat(cuentasCon("tarde." + marca + "@test.local")).isZero();
        assertThat(bandeja).isEmpty();
    }
}
