-- =====================================================================
-- Refresh token firmado (UT-HUI-REFRESH-BE-04). El token lleva el codigo de su
-- sesion y una firma HMAC: cualquier token anterior se reconoce sin historial,
-- asi que sobra `refresh_hash_anterior`.
-- =====================================================================

-- Las sesiones abiertas tienen tokens sin firma: dejan de valer una unica vez.
-- NO FORCE: con RLS forzado, un dueño sin superusuario actualizaria 0 filas
-- sin avisar. Va dentro de la transaccion de la migracion.
ALTER TABLE authentication.sesion NO FORCE ROW LEVEL SECURITY;

UPDATE authentication.sesion
SET estado = 'EXPIRADA', updated_at = now()
WHERE estado = 'ACTIVA';

ALTER TABLE authentication.sesion FORCE ROW LEVEL SECURITY;

-- La sesion se busca por su codigo, y solo despues de validar la firma.
DROP POLICY sesion_aislamiento ON authentication.sesion;

CREATE POLICY sesion_aislamiento ON authentication.sesion
    USING (
        usuario_codigo = nullif(current_setting('app.current_user', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
        OR codigo_sesion = nullif(current_setting('app.refresh_sid', true), '')::uuid
    )
    WITH CHECK (
        usuario_codigo = nullif(current_setting('app.current_user', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP INDEX authentication.ix_sesion_refresh_hash_anterior;
ALTER TABLE authentication.sesion DROP COLUMN refresh_hash_anterior;
