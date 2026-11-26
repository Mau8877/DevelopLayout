package com.example.backend.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

/**
 * Seguridad por método (`@PreAuthorize` en el service) y errores del cliente:
 * ninguno puede terminar en 500.
 *
 * * El controller y el service de prueba existen solo en este contexto: la
 * * plantilla todavía no tiene un endpoint real protegido por rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ErroresYPermisosTest.Config.class)
class ErroresYPermisosTest extends PostgresRuntimeIntegrationTest {

    private static final String BASE = "/api/v1/prueba-errores";

    @Autowired
    private MockMvc mvc;

    static class ServicioProtegido {

        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public void soloAdmin() {
        }
    }

    @RestController
    @RequestMapping(BASE)
    static class ControllerDePrueba {

        private final ServicioProtegido servicio;

        ControllerDePrueba(ServicioProtegido servicio) {
            this.servicio = servicio;
        }

        @GetMapping("/admin")
        void admin() {
            servicio.soloAdmin();
        }

        @GetMapping("/numero/{valor}")
        void numero(@PathVariable int valor) {
        }

        @GetMapping("/parametro")
        void parametro(@RequestParam String obligatorio) {
        }

        @PostMapping(path = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        void json() {
        }

        @GetMapping("/fallo")
        void fallo() {
            throw new IllegalStateException("detalle interno");
        }
    }

    @TestConfiguration
    static class Config {

        @Bean
        ServicioProtegido servicioProtegido() {
            return new ServicioProtegido();
        }

        @Bean
        ControllerDePrueba controllerDePrueba(ServicioProtegido servicio) {
            return new ControllerDePrueba(servicio);
        }
    }

    private static RequestPostProcessor conRol(String rol) {
        return user(UUID.randomUUID().toString()).roles(rol);
    }

    @Test
    void preAuthorize_conElRolRequerido_permiteElAcceso() throws Exception {
        mvc.perform(get(BASE + "/admin").with(conRol("SUPER_ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void preAuthorize_conRolInsuficiente_da403ConElSobre() throws Exception {
        mvc.perform(get(BASE + "/admin").with(conRol("USUARIO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_08"));
    }

    @Test
    void preAuthorize_sinSesion_da401() throws Exception {
        mvc.perform(get(BASE + "/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
    }

    @Test
    void rutaInexistente_da404() throws Exception {
        mvc.perform(get(BASE + "/no-existe").with(conRol("USUARIO")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_03"));
    }

    @Test
    void metodoNoPermitido_da405ConCabeceraAllow() throws Exception {
        mvc.perform(post(BASE + "/admin").with(conRol("USUARIO")).with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    @Test
    void parametroMalTipado_da400() throws Exception {
        mvc.perform(get(BASE + "/numero/abc").with(conRol("USUARIO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void parametroObligatorioAusente_da400() throws Exception {
        mvc.perform(get(BASE + "/parametro").with(conRol("USUARIO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void tipoDeContenidoNoSoportado_da415() throws Exception {
        mvc.perform(post(BASE + "/json").with(conRol("USUARIO")).with(csrf())
                        .contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void falloNoControlado_sigueDando500SinDetalles() throws Exception {
        mvc.perform(get(BASE + "/fallo").with(conRol("USUARIO")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("ERR_SYS_00"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado"));
    }
}
