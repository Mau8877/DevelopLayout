package com.example.backend.security;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Único punto que fija el contexto de Row Level Security de la conexión.
 *
 * * Usa `set_config(..., true)` (alcance de transacción): el valor muere con
 * * el commit/rollback y no se filtra a otro request que reuse la conexión
 * * del pool.
 * ! Exige transacción activa: fuera de una, el valor se perdería al instante
 * ! y el código fallaría cerrado de forma confusa, así que se falla fuerte.
 */
@Component
public class RlsContext {

    private final JdbcTemplate jdbc;

    public RlsContext(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void usarCorreoDeLogin(String email) {
        fijar("app.login_email", email);
    }

    /**
     * Deja ver la sesión que nombra un refresh token.
     *
     * ! Solo después de validar la firma del token (`RefreshTokens`).
     */
    public void usarRefreshSesion(UUID sesionCodigo) {
        fijar("app.refresh_sid", sesionCodigo.toString());
    }

    /**
     * Contexto de administrador: ve y revoca sesiones, lee usuarios y la bitácora.
     *
     * ! Solo desde services con `@PreAuthorize` de administrador ya verificado.
     */
    public void usarAdmin() {
        fijar("app.is_admin", "true");
    }

    public void usarUsuario(UUID codigo) {
        fijar("app.current_user", codigo.toString());
    }

    private void fijar(String clave, String valor) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("RlsContext requiere una transacción activa");
        }
        jdbc.queryForObject("SELECT set_config(?, ?, true)", String.class, clave, valor);
    }
}
