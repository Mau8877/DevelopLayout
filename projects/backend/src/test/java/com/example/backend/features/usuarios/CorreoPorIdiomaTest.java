package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mail.SimpleMailMessage;

/** El idioma de cada correo (CA07, CA08). */
class CorreoPorIdiomaTest extends InvitacionesIntegrationTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "en | You have been invited to the system | expires in 24 hours",
        "es | Te invitaron a usar el sistema      | vence en 24 horas",
    })
    void laInvitacionYSuReenvioSalenEnElIdiomaDelAdministrador(String idioma, String asunto, String texto)
            throws Exception {
        var alta = mvc.perform(como(admin, conJson(post("/api/v1/users"),
                        "{\"email\":\"idioma." + marca + "@test.local\",\"roles\":[\"USUARIO\"]}"))
                        .header("Accept-Language", idioma))
                .andExpect(status().isCreated());
        mvc.perform(como(admin, post("/api/v1/users/" + codigoCreado(alta) + "/resend-invitation"))
                        .header("Accept-Language", idioma))
                .andExpect(status().isOk());

        assertThat(bandeja).hasSize(2).allSatisfy(correo -> {
            assertThat(correo.getSubject()).isEqualTo(asunto);
            assertThat(correo.getText()).contains(texto, "http://localhost:5173/accept-invitation#token=");
        });
    }

    // * El envío ocurre en el hilo del ejecutor, que no tiene petición: el idioma viaja como dato.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "en | Reset your password     | expires in 30 minutes",
        "es | Restablece tu contraseña | vence en 30 minutos",
    })
    void laRecuperacionSaleEnElIdiomaDeQuienLaPide(String idioma, String asunto, String texto) throws Exception {
        Cuenta cuenta = crearCuenta("olvido", "USUARIO");

        mvc.perform(conJson(post("/api/v1/auth/password/forgot"), "{\"email\":\"" + cuenta.email() + "\"}")
                        .header("Accept-Language", idioma).with(csrf()))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(10)).until(() -> !bandeja.isEmpty());
        SimpleMailMessage correo = bandeja.getLast();
        assertThat(correo.getSubject()).isEqualTo(asunto);
        assertThat(correo.getText()).contains(texto, "http://localhost:5173/reset-password#token=");
    }
}
