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

/** Qué puede modificar `app_runtime` de una asignación de rol. */
@SpringBootTest
class RlsAdminRolesTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transacciones;

    private final UUID codigo = UUID.randomUUID();

    @BeforeEach
    void crearUsuarioConRol() {
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (?, ?, 'hash', 'ACTIVO')", codigo, "ro" + codigo + "@test.local");
        dueno().update("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                + "SELECT gen_random_uuid(), u.id, r.id FROM authentication.usuario u, authentication.rol r "
                + "WHERE u.codigo_usuario = ? AND r.nombre = 'USUARIO'", codigo);
    }

    private <T> T en(String variable, String valor, Supplier<T> accion) {
        return new TransactionTemplate(transacciones).execute(estado -> {
            estado.setRollbackOnly();
            jdbc.queryForObject("SELECT set_config(?, ?, true)", String.class, variable, valor);
            return accion.get();
        });
    }

    private int actualizar(String asignacion) {
        return jdbc.update("UPDATE authentication.usuario_rol SET " + asignacion + " WHERE usuario_id = "
                + "(SELECT id FROM authentication.usuario WHERE codigo_usuario = '" + codigo + "')");
    }

    @Test
    void elAdministradorActivaYDesactivaAsignaciones() {
        assertThat(en("app.is_admin", "true", () -> actualizar("is_active = false"))).isEqualTo(1);
    }

    @Test
    void conElContextoPropioNoSeModificaNingunaAsignacion() {
        assertThat(en("app.current_user", codigo.toString(), () -> actualizar("is_active = false"))).isZero();
    }

    @Test
    void niElAdministradorPuedeReapuntarlasOBorrarlas() {
        for (String cambio : new String[] {"usuario_id = usuario_id", "rol_id = rol_id"}) {
            assertThatThrownBy(() -> en("app.is_admin", "true", () -> actualizar(cambio)))
                    .hasStackTraceContaining("permission denied");
        }
        assertThatThrownBy(() -> en("app.is_admin", "true",
                () -> jdbc.update("DELETE FROM authentication.usuario_rol")))
                .hasStackTraceContaining("permission denied");
    }
}
