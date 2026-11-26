-- =====================================================================
-- El administrador puede LEER cualquier usuario (UT-HUI-SESIONESADMIN-BE-01).
-- Solo FOR SELECT: modificar sigue limitado por `usuario_aislamiento`.
-- `app.is_admin` lo fija únicamente RlsContext.usarAdmin(), desde services con
-- la autorización ya verificada.
-- =====================================================================

CREATE POLICY usuario_lectura_admin ON authentication.usuario
    FOR SELECT
    USING (current_setting('app.is_admin', true) = 'true');
