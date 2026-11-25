package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

@SpringBootTest
class UserDetailsServiceImplTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private UserDetailsServiceImpl service;

    @Autowired
    private RlsContext rlsContext;

    private void crearUsuario(String email, String estado, String rol, boolean rolActivo) {
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (?, ?, 'hash', ?)", UUID.randomUUID(), email, estado);
        dueno().update("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                + "SELECT gen_random_uuid(), u.id, r.id FROM authentication.usuario u, authentication.rol r "
                + "WHERE u.email = ? AND r.nombre = ?", email, rol);
        if (!rolActivo) {
            dueno().update("UPDATE authentication.usuario_rol SET is_active = false WHERE usuario_id = "
                    + "(SELECT id FROM authentication.usuario WHERE email = ?)", email);
        }
    }

    @Test
    void loadUserByUsername_conElAdminSembrado_devuelveRolSuperAdmin() {
        UserDetails u = service.loadUserByUsername("ADMIN@test.local");

        assertThat(u.getUsername()).isEqualTo("admin@test.local");
        assertThat(u.isEnabled()).isTrue();
        assertThat(u.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    void loadUserByUsername_conAsignacionDeRolInactiva_noDevuelveEseRol() {
        crearUsuario("sinrol@test.local", "ACTIVO", "USUARIO", false);

        assertThat(service.loadUserByUsername("sinrol@test.local").getAuthorities()).isEmpty();
    }

    @Test
    void loadUserByUsername_conUsuarioInactivo_vieneDeshabilitado() {
        crearUsuario("inactivo@test.local", "INACTIVO", "USUARIO", true);

        assertThat(service.loadUserByUsername("inactivo@test.local").isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_conCorreoInexistente_lanzaUsernameNotFound() {
        assertThatThrownBy(() -> service.loadUserByUsername("nadie@test.local"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rlsContext_sinTransaccion_lanzaExcepcion() {
        assertThatThrownBy(() -> rlsContext.usarUsuario(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> rlsContext.usarCorreoDeLogin("a@b.c"))
                .isInstanceOf(IllegalStateException.class);
    }
}
