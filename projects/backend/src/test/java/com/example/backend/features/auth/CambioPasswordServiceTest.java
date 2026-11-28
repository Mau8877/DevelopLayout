package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

import com.example.backend.features.auth.service.PasswordResetTokenService;

/** Cambio de contraseña con sesión iniciada (HU-INICIAL-CambiarPassword). */
@ExtendWith(OutputCaptureExtension.class)
class CambioPasswordServiceTest extends CambioPasswordIntegrationTest {

    @MockitoSpyBean
    private PasswordResetTokenService recuperaciones;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redis;

    @Test
    void cambiar_cierraTodasLasSesionesYSoloSirveLaContrasenaNueva(CapturedOutput salida) throws Exception {
        Tokens actual = loginMobile();
        Tokens web = loginWeb();
        String hashAnterior = hashGuardado();

        assertThat(estado(cambiar(actual, PASSWORD, NUEVA))).isEqualTo(200);

        assertThat(hashGuardado()).isNotEqualTo(hashAnterior).doesNotContain(NUEVA);
        assertThat(sesionesActivas()).isZero();
        assertThat(motivos()).containsExactly("CAMBIO_PASSWORD", "CAMBIO_PASSWORD");
        for (Tokens tokens : new Tokens[] {actual, web}) {
            assertThat(me(tokens)).isEqualTo(401);
            assertThat(renovar(tokens)).isEqualTo(401);
        }
        assertThat(login(PASSWORD)).isEqualTo(401);
        assertThat(login(NUEVA)).isEqualTo(200);
        assertThat(bitacora("AUTH_PASSWORD_CAMBIADA", "resultado")).isEqualTo("EXITO");
        assertThat(bitacora("AUTH_PASSWORD_CAMBIADA", "ip")).isEqualTo(ip);
        assertThat(bitacora("AUTH_PASSWORD_CAMBIADA", "user_agent")).isEqualTo("navegador-de-prueba");
        assertThat(bitacora("AUTH_PASSWORD_CAMBIADA", "detalle")).doesNotContain(NUEVA).doesNotContain(PASSWORD);
        assertThat(salida.getOut()).doesNotContain(NUEVA).doesNotContain(PASSWORD);
    }

    @Test
    void cambiar_conActualIncorrecta_responde400YNoCambiaNiCierraNada(CapturedOutput salida) throws Exception {
        Tokens tokens = loginMobile();
        String hashAnterior = hashGuardado();

        MvcResult res = cambiar(tokens, OTRA, NUEVA);

        assertThat(estado(res)).isEqualTo(400);
        assertThat(error(res)).isEqualTo("ERR_AUTH_12");
        assertThat(hashGuardado()).isEqualTo(hashAnterior);
        assertThat(sesionesActivas()).isEqualTo(1);
        assertThat(me(tokens)).isEqualTo(200);
        assertThat(bitacora("AUTH_PASSWORD_CAMBIO_FALLIDO", "resultado")).isEqualTo("FALLO");
        assertThat(bitacora("AUTH_PASSWORD_CAMBIO_FALLIDO", "ip")).isEqualTo(ip);
        assertThat(bitacora("AUTH_PASSWORD_CAMBIO_FALLIDO", "detalle")).doesNotContain(OTRA);
        assertThat(salida.getOut()).doesNotContain(OTRA).doesNotContain(NUEVA);
        esperarEjecutorLibre();
        assertThat(bandeja).isEmpty();
    }

    @Test
    void cambiar_conLaMismaContrasena_responde400SinCerrarSesiones() throws Exception {
        Tokens tokens = loginMobile();

        MvcResult res = cambiar(tokens, PASSWORD, PASSWORD);

        assertThat(estado(res)).isEqualTo(400);
        assertThat(error(res)).isEqualTo("ERR_AUTH_06");
        assertThat(me(tokens)).isEqualTo(200);
    }

    @Test
    void cambiar_trasCincoFallos_bloqueaSoloElCambio() throws Exception {
        Tokens tokens = loginMobile();
        for (int i = 0; i < 5; i++) {
            assertThat(error(cambiar(tokens, OTRA, NUEVA))).isEqualTo("ERR_AUTH_12");
        }

        MvcResult res = cambiar(tokens, PASSWORD, NUEVA);

        assertThat(estado(res)).isEqualTo(423);
        assertThat(error(res)).isEqualTo("ERR_AUTH_13");
        assertThat(me(tokens)).isEqualTo(200);
        assertThat(login(PASSWORD)).isEqualTo(200);
    }

    @Test
    void cambiar_levantaElBloqueoDeLogin() throws Exception {
        Tokens tokens = loginMobile();
        for (int i = 0; i < 5; i++) {
            assertThat(login(OTRA)).isEqualTo(401);
        }
        assertThat(login(PASSWORD)).isEqualTo(423);

        assertThat(estado(cambiar(tokens, PASSWORD, NUEVA))).isEqualTo(200);

        assertThat(login(NUEVA)).isEqualTo(200);
    }

    @Test
    void cambiar_anulaElEnlaceDeRecuperacionPendiente() throws Exception {
        Tokens tokens = loginMobile();
        String enlace = recuperaciones.emitir(codigoUsuario());

        assertThat(estado(cambiar(tokens, PASSWORD, NUEVA))).isEqualTo(200);

        assertThat(recuperaciones.consultar(enlace)).isEmpty();
    }

    @Test
    void cambiar_siNoSePuedenAnularLosEnlaces_noCambiaNada() throws Exception {
        Tokens tokens = loginMobile();
        String hashAnterior = hashGuardado();
        doThrow(new IllegalStateException("sin Redis")).when(recuperaciones).anular(any(UUID.class));

        assertThat(estado(cambiar(tokens, PASSWORD, NUEVA))).isEqualTo(500);

        assertThat(hashGuardado()).isEqualTo(hashAnterior);
        assertThat(sesionesActivas()).isEqualTo(1);
        assertThat(me(tokens)).isEqualTo(200);
        assertThat(bitacora("AUTH_PASSWORD_CAMBIADA", "resultado")).isNull();
    }

    @Test
    void cambiar_enviaElAvisoEnElIdiomaDeLaPeticion() throws Exception {
        Tokens tokens = loginMobile();

        assertThat(estado(mvc.perform(cambioMobile(tokens, cuerpo(PASSWORD, NUEVA)).header("Accept-Language", "en"))
                .andReturn())).isEqualTo(200);
        esperarEjecutorLibre();

        assertThat(bandeja).hasSize(1);
        SimpleMailMessage aviso = bandeja.getFirst();
        assertThat(aviso.getTo()).containsExactly(email);
        assertThat(aviso.getSubject()).isEqualTo("Your password was changed");
        assertThat(aviso.getText()).doesNotContain(NUEVA, PASSWORD, "http");
    }

    @Test
    void cambiar_conElCorreoCaido_mantieneElCambioYLoRegistraSinDatos(CapturedOutput salida) throws Exception {
        Tokens tokens = loginMobile();
        doThrow(new MailSendException("rechazado " + email)).when(mail).send(any(SimpleMailMessage.class));

        assertThat(estado(cambiar(tokens, PASSWORD, NUEVA))).isEqualTo(200);
        esperarEjecutorLibre();

        assertThat(login(NUEVA)).isEqualTo(200);
        assertThat(salida.getOut()).contains("No se pudo enviar el aviso de cambio de contraseña")
                .doesNotContain("rechazado " + email);
    }

    @Test
    void cambiar_soloAfectaALaCuentaDelToken() throws Exception {
        Tokens tokens = loginMobile();
        String ajena = "ajena-" + email;
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "SELECT gen_random_uuid(), ?, password_hash, 'ACTIVO' FROM authentication.usuario WHERE email = ?",
                ajena, email);
        String hashAjeno = hashGuardado();

        // * No hay forma de nombrar otra cuenta: una propiedad extra se rechaza.
        MvcResult conOtraCuenta = mvc.perform(cambioMobile(tokens,
                "{\"passwordActual\":\"" + PASSWORD + "\",\"passwordNueva\":\"" + NUEVA + "\",\"email\":\"" + ajena
                        + "\"}")).andReturn();
        assertThat(estado(conOtraCuenta)).isEqualTo(400);
        assertThat(estado(cambiar(tokens, PASSWORD, NUEVA))).isEqualTo(200);

        assertThat(dueno().queryForObject("SELECT password_hash FROM authentication.usuario WHERE email = ?",
                String.class, ajena)).isEqualTo(hashAjeno);
    }
}
