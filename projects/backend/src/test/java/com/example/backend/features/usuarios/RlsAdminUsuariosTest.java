package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

/** Qué puede hacer `app_runtime` con usuarios y asignaciones en cada contexto. */
@SpringBootTest
class RlsAdminUsuariosTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transacciones;

    private final UUID codigo = UUID.randomUUID();
    private String email;

    @BeforeEach
    void crearUsuario() {
        email = "r" + codigo.toString().substring(0, 8) + "@test.local";
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (?, ?, 'hash-original', 'ACTIVO')", codigo, email);
    }

    /** Cada llamada es una transacción propia de `app_runtime` que se deshace al terminar. */
    private <T> T en(String variable, String valor, Supplier<T> accion) {
        return new TransactionTemplate(transacciones).execute(estado -> {
            estado.setRollbackOnly();
            if (variable != null) {
                jdbc.queryForObject("SELECT set_config(?, ?, true)", String.class, variable, valor);
            }
            return accion.get();
        });
    }

    private <T> T comoAdmin(Supplier<T> accion) {
        return en("app.is_admin", "true", accion);
    }

    private int actualizar(String asignacion) {
        return jdbc.update("UPDATE authentication.usuario SET " + asignacion + " WHERE codigo_usuario = ?", codigo);
    }

    private int insertarUsuario() {
        UUID nuevo = UUID.randomUUID();
        return jdbc.update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado, "
                + "invitacion_pendiente) VALUES (?, ?, 'x', 'ACTIVO', true)", nuevo, "n" + nuevo + "@test.local");
    }

    private int insertarRol(String rol) {
        return jdbc.update("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                + "SELECT gen_random_uuid(), u.id, r.id FROM authentication.usuario u, authentication.rol r "
                + "WHERE u.codigo_usuario = ? AND r.nombre = ?", codigo, rol);
    }

    @Test
    void sinContexto_noSeLeeNiSeEscribe() {
        assertThat(en(null, null, () -> jdbc.queryForObject(
                "SELECT count(*) FROM authentication.usuario", Integer.class))).isZero();
        assertThat(en(null, null, () -> actualizar("estado = 'INACTIVO'"))).isZero();
        assertThatThrownBy(() -> en(null, null, this::insertarUsuario))
                .hasStackTraceContaining("row-level security");
    }

    @Test
    void contextoDeAdmin_daDeAltaYCambiaElEstado() {
        assertThat(comoAdmin(this::insertarUsuario)).isEqualTo(1);
        assertThat(comoAdmin(() -> actualizar("estado = 'INACTIVO', invitacion_pendiente = true"))).isEqualTo(1);
        assertThat(comoAdmin(() -> insertarRol("USUARIO"))).isEqualTo(1);
    }

    @Test
    void contextoDeAdmin_noCambiaCorreosNiContrasenasAjenasNiBorra() {
        assertThatThrownBy(() -> comoAdmin(() -> actualizar("email = 'otro@test.local'")))
                .hasStackTraceContaining("permission denied");
        assertThatThrownBy(() -> comoAdmin(() -> actualizar("password_hash = 'robada'"))) // gitleaks:allow
                .hasStackTraceContaining("Solo el propio usuario");
        assertThatThrownBy(() -> comoAdmin(() -> jdbc.update("DELETE FROM authentication.usuario")))
                .hasStackTraceContaining("permission denied");
        assertThatThrownBy(() -> comoAdmin(() -> jdbc.update("DELETE FROM authentication.usuario_rol")))
                .hasStackTraceContaining("permission denied");
    }

    @Test
    void contextoPropio_cambiaSuContrasenaPeroNoSeDaRolesNiCreaUsuarios() {
        assertThat(en("app.current_user", codigo.toString(), () -> actualizar("password_hash = 'nueva'")))
                .isEqualTo(1);
        assertThatThrownBy(() -> en("app.current_user", codigo.toString(), () -> insertarRol("SUPER_ADMIN")))
                .hasStackTraceContaining("row-level security");
        assertThatThrownBy(() -> en("app.current_user", codigo.toString(), this::insertarUsuario))
                .hasStackTraceContaining("row-level security");
    }

    @Test
    void correoDelLogin_soloLee() {
        assertThat(en("app.login_email", email, () -> jdbc.queryForObject(
                "SELECT count(*) FROM authentication.usuario", Integer.class))).isEqualTo(1);
        assertThat(en("app.login_email", email, () -> actualizar("estado = 'INACTIVO'"))).isZero();
    }
}
