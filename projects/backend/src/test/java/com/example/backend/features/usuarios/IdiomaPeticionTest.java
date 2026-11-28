package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import org.junit.jupiter.api.Test;

/** El servidor responde en el idioma pedido, con el mismo código de error (CA05, CA06). */
class IdiomaPeticionTest extends AdminUsuariosIntegrationTest {

    private static MockHttpServletRequestBuilder en(MockHttpServletRequestBuilder peticion, String idioma) {
        return idioma.isEmpty() ? peticion : peticion.header("Accept-Language", idioma);
    }

    private static void esperar(ResultActions respuesta, int estado, String codigo, String mensaje) throws Exception {
        respuesta.andExpect(status().is(estado))
                .andExpect(jsonPath("$.error").value(codigo))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "en                 | You need to sign in",
        "en-US,en;q=0.9     | You need to sign in",
        "es-BO              | Debes iniciar sesión",
        "fr-FR              | Debes iniciar sesión",
        "''                 | Debes iniciar sesión",
        "../../etc/passwd   | Debes iniciar sesión",
    })
    void sinSesion_elIdiomaSaleDeLaCabeceraYPorDefectoEsEspanol(String idioma, String mensaje) throws Exception {
        esperar(mvc.perform(en(get("/api/v1/auth/me"), idioma)), 401, "ERR_AUTH_07", mensaje);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"en | Access denied", "es | Acceso denegado"})
    void sinPermiso_403EnElIdiomaPedido(String idioma, String mensaje) throws Exception {
        Cuenta comun = conSesion(crearCuenta("comun", "USUARIO"));

        esperar(mvc.perform(en(como(comun, conJson(post("/api/v1/users/search"), "{}")), idioma)),
                403, "ERR_AUTH_08", mensaje);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "en | Email is required                          | Incorrect email or password | The requested resource does not exist",
        "es | El correo es obligatorio                   | Correo o contraseña incorrectos | El recurso solicitado no existe",
    })
    void validacionesCredencialesYRutas(String idioma, String validacion, String credenciales, String noExiste)
            throws Exception {
        esperar(mvc.perform(en(conJson(post("/api/v1/auth/login"),
                "{\"email\":\"\",\"password\":\"ClaveSegura123\"}"), idioma).with(csrf())), // gitleaks:allow
                400, "ERR_SYS_01", validacion);
        // * Correo inexistente y contraseña incorrecta: texto idéntico en cada idioma.
        for (String correo : new String[] {admin.email(), "nadie." + marca + "@test.local"}) {
            esperar(mvc.perform(en(conJson(post("/api/v1/auth/login"),
                    "{\"email\":\"" + correo + "\",\"password\":\"OtraClave999\"}"), idioma).with(csrf())), // gitleaks:allow
                    401, "ERR_AUTH_01", credenciales);
        }
        esperar(mvc.perform(en(como(admin, get("/api/v1/no-existe")), idioma)), 404, "ERR_SYS_03", noExiste);
    }

    @Test
    void erroresDeNegocioYExitosEnIngles_conSusArgumentos() throws Exception {
        esperar(mvc.perform(en(como(admin, post("/api/v1/users/" + admin.codigo() + "/deactivate")), "en")),
                409, "ERR_US_04", "You cannot deactivate your own account");
        esperar(mvc.perform(en(como(admin, conJson(post("/api/v1/users/search"), "{\"pageSize\":101}")), "en")),
                400, "ERR_SYS_01", "The pagination is not valid: page starts at 1 and pageSize must be between 1 and 100");
        mvc.perform(en(como(admin, conJson(post("/api/v1/users/search"), "{}")), "en"))
                .andExpect(jsonPath("$.message").value("Users retrieved"));
    }

    @Test
    void elPreflightDeCorsAceptaLaCabeceraDeIdioma() throws Exception {
        String permitidas = mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "accept-language"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Headers"))
                .andReturn().getResponse().getHeader("Access-Control-Allow-Headers");

        assertThat(permitidas).containsIgnoringCase("accept-language");
    }
}
