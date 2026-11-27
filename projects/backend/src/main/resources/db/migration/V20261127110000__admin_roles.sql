-- =====================================================================
-- Cambio de roles (UT-HUI-ROLES-BE-01).
-- Quitar un rol desactiva la asignación y volver a darlo la reactiva: el
-- administrador necesita modificar asignaciones, pero no reapuntarlas a otro
-- usuario o a otro rol.
-- =====================================================================

CREATE POLICY usuario_rol_edicion_admin ON authentication.usuario_rol
    FOR UPDATE
    USING (current_setting('app.is_admin', true) = 'true')
    WITH CHECK (current_setting('app.is_admin', true) = 'true');

REVOKE UPDATE ON authentication.usuario_rol FROM app_runtime;
GRANT UPDATE (is_active, updated_at, deleted_at) ON authentication.usuario_rol TO app_runtime;
