package com.example.backend.features.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import jakarta.servlet.http.Cookie;

/** Los tres caminos que fijan una contraseña rechazan las muy usadas, en el idioma pedido. */
class PasswordComunTest extends AuthSesionIntegrationTest {

    private static final String COMUN = "Password"; // gitleaks:allow
    private static final String MENSAJE_ES = "Esa contraseña es demasiado común y fácil de adivinar. Elige otra";
    private static final String MENSAJE_EN = "That password is too common and easy to guess. Choose a different one";

    @Test
    void restablecerYAceptarInvitacion_conUnaClaveComun_son400() throws Exception {
        String body = "{\"token\":\"enlace-inventado\",\"password\":\"" + COMUN + "\"}";

        for (String ruta : new String[] {"/api/v1/auth/password/reset", "/api/v1/auth/invitation/accept"}) {
            rechazada(enviar(ruta, "es", body), MENSAJE_ES);
            rechazada(enviar(ruta, "en", body), MENSAJE_EN);
        }
    }

    @Test
    void cambiarLaPropia_porUnaClaveComun_es400YLaSesionSigue() throws Exception {
        Tokens sesion = loginWeb();
        String body = "{\"passwordActual\":\"" + PASSWORD + "\",\"passwordNueva\":\"" + COMUN + "\"}";

        rechazada(mvc.perform(post("/api/v1/auth/password/change").with(csrf())
                .cookie(new Cookie("accessToken", sesion.access()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)), MENSAJE_ES);

        assertAccess(sesion.access(), 200);
    }

    @Test
    void iniciarSesion_noAplicaLaRegla_paraNoDejarFueraANadie() throws Exception {
        // * Credenciales incorrectas (401), no un rechazo de formato (400).
        enviar("/api/v1/auth/login", "es", "{\"email\":\"nadie@test.local\",\"password\":\"" + COMUN + "\"}")
                .andExpect(status().isUnauthorized());
    }

    private ResultActions enviar(String ruta, String idioma, String body) throws Exception {
        return mvc.perform(post(ruta).header("X-Client-Type", "mobile").header("Accept-Language", idioma)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static void rechazada(ResultActions respuesta, String mensaje) throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
                .andExpect(jsonPath("$.message").value(mensaje));
    }
}
