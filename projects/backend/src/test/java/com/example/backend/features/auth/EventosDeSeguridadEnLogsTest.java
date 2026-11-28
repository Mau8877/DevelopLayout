package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import jakarta.servlet.http.Cookie;

/** Lo rechazado deja rastro (A09), sin datos personales. */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {"app.ratelimit.forgot-capacity=1", "app.ratelimit.forgot-refill-per-minute=1"})
class EventosDeSeguridadEnLogsTest extends AuthSesionIntegrationTest {

    @Test
    void unAccesoSinPermiso_quedaRegistradoConQuienYQue(CapturedOutput salida) throws Exception {
        Tokens comun = loginWeb();
        String codigo = dueno().queryForObject(
                "SELECT codigo_usuario::text FROM authentication.usuario WHERE email = ?", String.class, email);

        mvc.perform(get("/api/v1/audit-log").cookie(new Cookie("accessToken", comun.access())))
                .andExpect(status().isForbidden());

        assertThat(salida.getAll()).contains("Acceso denegado metodo=GET ruta=/api/v1/audit-log usuarioCodigo="
                + codigo + " errorCode=ERR_AUTH_08");
        assertThat(salida.getAll()).doesNotContain(email, comun.access());
    }

    @Test
    void superarUnLimite_quedaRegistradoSinElContenidoDeLaPeticion(CapturedOutput salida) throws Exception {
        String body = "{\"email\":\"" + email + "\"}";
        for (int esperado : new int[] {200, 429}) {
            mvc.perform(post("/api/v1/auth/password/forgot").header("X-Client-Type", "mobile")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().is(esperado));
        }

        assertThat(salida.getAll()).contains(
                "Límite de peticiones alcanzado metodo=POST ruta=/api/v1/auth/password/forgot errorCode=ERR_SYS_02");
        assertThat(salida.getAll()).doesNotContain(email);
    }
}
