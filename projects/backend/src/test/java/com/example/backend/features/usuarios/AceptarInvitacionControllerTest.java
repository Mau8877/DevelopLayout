package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.example.backend.features.auth.service.InvitacionTokenService;
import com.example.backend.features.auth.service.PasswordResetTokenService;
import com.example.backend.features.auth.service.RecuperacionService;

/** Aceptar la invitación, directamente o al recuperar la contraseña (CA07, CA08, CA19). */
class AceptarInvitacionControllerTest extends InvitacionesIntegrationTest {

    private static final String ELEGIDA = "ClaveElegida789"; // gitleaks:allow
    private static final String RUTA = "/api/v1/auth/invitation/accept";

    @Autowired
    private InvitacionTokenService invitaciones;

    @Autowired
    private PasswordResetTokenService recuperaciones;

    @Autowired
    private RecuperacionService recuperacion;

    private static String cuerpo(String token, String password) {
        return "{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}";
    }

    private ResultActions aceptar(String token, String password) throws Exception {
        return mvc.perform(conJson(post(RUTA), cuerpo(token, password)).with(csrf()));
    }

    private record Invitada(UUID codigo, String email, String token) {
    }

    private Invitada invitada() throws Exception {
        String email = "inv." + marca + "@test.local";
        UUID codigo = codigoCreado(invitar(email).andExpect(status().isCreated()));
        return new Invitada(codigo, email, ultimoToken());
    }

    private void rechazado(String token) throws Exception {
        aceptar(token, ELEGIDA).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_11"));
    }

    @Test
    void guardaLaContrasenaYPermiteIniciarSesion() throws Exception {
        Invitada persona = invitada();
        String recuperacionSuelta = recuperaciones.emitir(persona.codigo());

        aceptar(persona.token(), ELEGIDA).andExpect(status().isOk());

        login(persona.email(), ELEGIDA).andExpect(status().isOk());
        assertThat(columna(persona.codigo(), "invitacion_pendiente")).isEqualTo("false");
        assertThat(eventosSobre("AUTH_INVITACION_ACEPTADA", persona.codigo())).isEqualTo(1);
        assertThat(recuperaciones.consultar(recuperacionSuelta)).isEmpty();
        rechazado(persona.token());
    }

    @Test
    void unaContrasenaFueraDeLaPoliticaNoGastaElEnlace() throws Exception {
        Invitada persona = invitada();

        aceptar(persona.token(), "corta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        assertThat(invitaciones.consultar(persona.token())).contains(persona.codigo());
        aceptar(persona.token(), ELEGIDA).andExpect(status().isOk());
    }

    @Test
    void enlacesQueNoSirven_noCambianNada() throws Exception {
        Invitada persona = invitada();
        String hash = columna(persona.codigo(), "password_hash");

        rechazado("inventado");
        rechazado(recuperaciones.emitir(persona.codigo()));

        invitaciones.emitir(persona.codigo());
        rechazado(persona.token());

        String anulado = invitaciones.emitir(persona.codigo());
        invitaciones.anular(persona.codigo());
        rechazado(anulado);

        String deInactiva = invitaciones.emitir(persona.codigo());
        dueno().update("UPDATE authentication.usuario SET estado = 'INACTIVO' WHERE codigo_usuario = ?",
                persona.codigo());
        rechazado(deInactiva);

        assertThat(columna(persona.codigo(), "password_hash")).isEqualTo(hash);
        assertThat(columna(persona.codigo(), "invitacion_pendiente")).isEqualTo("true");
    }

    @Test
    void dosAceptacionesDelMismoEnlace_soloUnaGana() throws Exception {
        Invitada persona = invitada();
        List<Callable<Integer>> intentos = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            intentos.add(() -> aceptar(persona.token(), ELEGIDA).andReturn().getResponse().getStatus());
        }

        List<Integer> estados = new ArrayList<>();
        try (var hilos = Executors.newFixedThreadPool(2)) {
            for (var futuro : hilos.invokeAll(intentos)) {
                estados.add(futuro.get());
            }
        }

        assertThat(estados).containsExactlyInAnyOrder(200, 400);
        assertThat(eventosSobre("AUTH_INVITACION_ACEPTADA", persona.codigo())).isEqualTo(1);
    }

    @Test
    void enWebSinCsrfEs403() throws Exception {
        mvc.perform(conJson(post(RUTA), cuerpo("x", ELEGIDA))).andExpect(status().isForbidden());
    }

    @Test
    void restablecerLaContrasenaTambienAceptaLaInvitacion() throws Exception {
        Invitada persona = invitada();

        recuperacion.restablecer(recuperaciones.emitir(persona.codigo()), ELEGIDA);

        assertThat(columna(persona.codigo(), "invitacion_pendiente")).isEqualTo("false");
        assertThat(eventosSobre("AUTH_INVITACION_ACEPTADA", persona.codigo())).isEqualTo(1);
        assertThat(eventosSobre("AUTH_PASSWORD_RESTABLECIDA", persona.codigo())).isEqualTo(1);
        assertThat(invitaciones.consultar(persona.token())).isEmpty();
        rechazado(persona.token());
        login(persona.email(), ELEGIDA).andExpect(status().isOk());
    }
}
