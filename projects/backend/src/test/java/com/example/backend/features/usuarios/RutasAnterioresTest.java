package com.example.backend.features.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las URL pasaron a inglés sin redirecciones: las anteriores no existen, ni para un administrador. */
class RutasAnterioresTest extends AdminUsuariosIntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/bitacora", "/api/v1/auth/sesiones", "/api/v1/usuarios"})
    void unaRutaAnteriorDeLectura_noExiste(String ruta) throws Exception {
        mvc.perform(como(admin, get(ruta))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/usuarios/buscar", "/api/v1/admin/sesiones/buscar",
        "/api/v1/auth/sesiones/cerrar-otras"})
    void unaRutaAnteriorDeEscritura_noExiste(String ruta) throws Exception {
        mvc.perform(como(admin, conJson(post(ruta), "{}"))).andExpect(status().isNotFound());
    }
}
