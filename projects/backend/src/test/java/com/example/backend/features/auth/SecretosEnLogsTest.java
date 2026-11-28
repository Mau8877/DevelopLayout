package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Ningún secreto sale en los logs, ni con el registro más detallado de la capa web.
 *
 * * Es el escenario real de quien depura: con DEBUG, Spring escribe el `toString()`
 * * de cada body que lee y responde; con `log-resolved-exception` (lo activa
 * * devtools) escribe las excepciones, que traen el valor rechazado.
 */
@ExtendWith(OutputCaptureExtension.class)
// * Sin el volcado de MockMvc: es de las pruebas, trae el body entero y ensuciaría la salida revisada.
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestPropertySource(properties = "spring.mvc.log-resolved-exception=true")
class SecretosEnLogsTest extends AuthSesionIntegrationTest {

    // * Valores inventados, fáciles de buscar en la salida.
    private static final String CLAVE_VALIDA = "SondaClaveValida-93"; // gitleaks:allow
    private static final String CLAVE_CORTA = "Sonda7q"; // gitleaks:allow
    private static final String ENLACE = "SondaEnlaceDeUnSoloUso-41"; // gitleaks:allow
    private static final String REFRESH_FALSO = "SondaRefreshInventado-58"; // gitleaks:allow

    private final Logger capaWeb = (Logger) LoggerFactory.getLogger("org.springframework.web");
    private Level nivelAnterior;

    // ! El nivel de log es de toda la JVM: si no se restaura, las demás pruebas quedan en DEBUG.
    @BeforeEach
    void activarRegistroDetallado() {
        nivelAnterior = capaWeb.getLevel();
        capaWeb.setLevel(Level.DEBUG);
    }

    @AfterEach
    void restaurarElNivel() {
        capaWeb.setLevel(nivelAnterior);
    }

    @Test
    void loginYRenovacionMobile_noEscribenLaClaveNiLosTokens(CapturedOutput salida) throws Exception {
        Tokens primeros = loginMobile();
        Tokens nuevos = tokensDelBody(mvc.perform(mobile("refresh", primeros.refresh())).andReturn());

        // Si el registro detallado no estuviera activo, la prueba pasaría sin comprobar nada.
        assertThat(salida.getAll()).contains("LoginRequest", "RefreshRequest", "ApiResponse");
        assertThat(salida.getAll()).doesNotContain(PASSWORD, primeros.access(), primeros.refresh(),
                nuevos.access(), nuevos.refresh());
    }

    @Test
    void enlacesDeUnSoloUso_noEscribenElEnlaceNiLaClave(CapturedOutput salida) throws Exception {
        String body = "{\"token\":\"" + ENLACE + "\",\"password\":\"" + CLAVE_VALIDA + "\"}";
        enviar("/api/v1/auth/password/reset", body);
        enviar("/api/v1/auth/invitation/accept", body);
        mvc.perform(mobile("logout", REFRESH_FALSO));

        assertThat(salida.getAll()).contains("ResetPasswordRequest", "AceptarInvitacionRequest");
        assertThat(salida.getAll()).doesNotContain(ENLACE, CLAVE_VALIDA, REFRESH_FALSO);
    }

    @Test
    void datosRechazadosPorLaValidacion_noEscribenElValorRechazado(CapturedOutput salida) throws Exception {
        String corta = "\"password\":\"" + CLAVE_CORTA + "\"";
        rechazar("/api/v1/auth/login", "{\"email\":\"sonda@test.local\"," + corta + "}");
        rechazar("/api/v1/auth/password/reset", "{\"token\":\"" + ENLACE + "\"," + corta + "}");
        rechazar("/api/v1/auth/invitation/accept", "{\"token\":\"" + ENLACE + "\"," + corta + "}");

        // La validación rechazó los tres (400, lo comprueba `rechazar`) y se registró la petición.
        assertThat(salida.getAll()).contains("AuthController#login", "AuthController#restablecerPassword",
                "AuthController#aceptarInvitacion");
        assertThat(salida.getAll()).doesNotContain(CLAVE_CORTA, ENLACE);
    }

    private void rechazar(String ruta, String body) throws Exception {
        enviar(ruta, body).andExpect(status().isBadRequest());
    }

    private ResultActions enviar(String ruta, String body) throws Exception {
        return mvc.perform(post(ruta).header("X-Client-Type", "mobile")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
