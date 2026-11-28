package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.ResultActions;

import com.example.backend.features.usuarios.repository.UsuarioBusquedaRepository;
import com.example.backend.features.usuarios.service.UsuarioAdminService;

import jakarta.servlet.http.Cookie;

/** Listado y búsqueda (CA01, CA02, CA18, CA21). */
class UsuarioBusquedaControllerTest extends AdminUsuariosIntegrationTest {

    @Autowired
    private UsuarioAdminService servicio;

    private ResultActions buscar(String criterios) throws Exception {
        return mvc.perform(como(admin, conJson(post("/api/v1/users/search"), criterios)));
    }

    @Test
    void devuelveCadaCuentaConRolesEstadoYFechaSinDatosInternos() throws Exception {
        Cuenta otra = crearCuenta("ana", "USUARIO", "SUPER_ADMIN");

        ResultActions res = buscar("{\"search\":\"ana." + marca + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].codigo").value(otra.codigo().toString()))
                .andExpect(jsonPath("$.data[0].email").value(otra.email()))
                .andExpect(jsonPath("$.data[0].roles[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data[0].roles[1]").value("USUARIO"))
                .andExpect(jsonPath("$.data[0].estado").value("ACTIVO"))
                .andExpect(jsonPath("$.data[0].invitacionPendiente").value(false))
                .andExpect(jsonPath("$.data[0].creadoEn").isNotEmpty())
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20));

        assertThat(res.andReturn().getResponse().getContentAsString()).doesNotContainIgnoringCase("hash")
                .doesNotContainIgnoringCase("password");
    }

    @Test
    void paginaDelAltaMasRecienteALaMasAntigua() throws Exception {
        crearCuenta("uno", "USUARIO");
        Cuenta dos = crearCuenta("dos", "USUARIO");
        Cuenta tres = crearCuenta("tres", "USUARIO");

        buscar("{\"search\":\"" + marca + "\",\"pageSize\":1}")
                .andExpect(jsonPath("$.data[0].email").value(tres.email()))
                .andExpect(jsonPath("$.meta.totalItems").value(4))
                .andExpect(jsonPath("$.meta.totalPages").value(4));
        buscar("{\"search\":\"" + marca + "\",\"page\":2,\"pageSize\":1}")
                .andExpect(jsonPath("$.data[0].email").value(dos.email()));
        buscar("{\"pageSize\":101}").andExpect(status().isBadRequest());
    }

    @Test
    void combinaTextoEstadoYRol() throws Exception {
        Cuenta activa = crearCuenta("activa", "USUARIO");
        Cuenta inactiva = crearCuenta("inactiva", "USUARIO");
        dueno().update("UPDATE authentication.usuario SET estado = 'INACTIVO' WHERE codigo_usuario = ?",
                inactiva.codigo());

        buscar("{\"search\":\"" + marca.toUpperCase() + "\",\"estado\":\"INACTIVO\"}")
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].email").value(inactiva.email()));
        buscar("{\"search\":\"" + marca + "\",\"rol\":\"SUPER_ADMIN\"}")
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].email").value(admin.email()));
        buscar("{\"search\":\"" + activa.email() + "\",\"rol\":\"SUPER_ADMIN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void losComodinesSeBuscanComoTexto() throws Exception {
        buscar("{\"search\":\"%" + marca + "\"}").andExpect(jsonPath("$.data.length()").value(0));
        buscar("{\"search\":\"admin_" + marca + "\"}").andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void criteriosInvalidosOCamposDeMas_son400() throws Exception {
        buscar("{\"estado\":\"BORRADO\"}").andExpect(status().isBadRequest());
        buscar("{\"passwordHash\":\"x\"}").andExpect(status().isBadRequest());
    }

    @Test
    void sinSesion401_sinRol403_ySinCsrf403() throws Exception {
        Cuenta comun = conSesion(crearCuenta("comun", "USUARIO"));

        mvc.perform(conJson(post("/api/v1/users/search"), "{}").with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(como(comun, conJson(post("/api/v1/users/search"), "{}")))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(post("/api/v1/users/search"), "{}")
                        .cookie(new Cookie("accessToken", admin.access())))
                .andExpect(status().isForbidden());
    }

    @Test
    void elServiceTambienDeniegaSinPasarPorHttp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
        try {
            assertThatThrownBy(() -> servicio.buscar(new UsuarioBusquedaRepository.Filtro(null, null, null),
                    PageRequest.of(0, 20)))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
