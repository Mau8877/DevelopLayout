-- =====================================================================
-- Administración de usuarios (UT-HUI-USUARIOS-BE-01).
-- El administrador puede dar de alta y cambiar el estado, y nada más. Se
-- cierran además dos permisos más amplios de lo necesario: escribir usuarios
-- con el correo del login e insertarse roles con el contexto propio.
-- =====================================================================

-- Mientras es true, la persona todavía no eligió su contraseña.
ALTER TABLE authentication.usuario
    ADD COLUMN invitacion_pendiente BOOLEAN NOT NULL DEFAULT false;

-- ---------------------------------------------------------------------
-- usuario
-- ---------------------------------------------------------------------
DROP POLICY usuario_aislamiento ON authentication.usuario;

CREATE POLICY usuario_lectura_propia ON authentication.usuario
    FOR SELECT
    USING (
        codigo_usuario = nullif(current_setting('app.current_user', true), '')::uuid
        OR lower(email) = lower(nullif(current_setting('app.login_email', true), ''))
    );

CREATE POLICY usuario_edicion_propia ON authentication.usuario
    FOR UPDATE
    USING (codigo_usuario = nullif(current_setting('app.current_user', true), '')::uuid)
    WITH CHECK (codigo_usuario = nullif(current_setting('app.current_user', true), '')::uuid);

CREATE POLICY usuario_alta_admin ON authentication.usuario
    FOR INSERT
    WITH CHECK (current_setting('app.is_admin', true) = 'true');

CREATE POLICY usuario_edicion_admin ON authentication.usuario
    FOR UPDATE
    USING (current_setting('app.is_admin', true) = 'true')
    WITH CHECK (current_setting('app.is_admin', true) = 'true');

-- Alta del administrador inicial: la hace el dueño que migra (TO CURRENT_USER),
-- que en una base administrada no ignora RLS.
CREATE POLICY usuario_alta_dueno ON authentication.usuario
    FOR INSERT
    TO CURRENT_USER
    WITH CHECK (codigo_usuario = nullif(current_setting('app.current_user', true), '')::uuid);

-- ---------------------------------------------------------------------
-- usuario_rol
-- ---------------------------------------------------------------------
DROP POLICY usuario_rol_aislamiento ON authentication.usuario_rol;

CREATE POLICY usuario_rol_lectura_propia ON authentication.usuario_rol
    FOR SELECT
    USING (usuario_id IN (
        SELECT u.id FROM authentication.usuario u
        WHERE u.codigo_usuario = nullif(current_setting('app.current_user', true), '')::uuid
    ));

CREATE POLICY usuario_rol_lectura_admin ON authentication.usuario_rol
    FOR SELECT
    USING (current_setting('app.is_admin', true) = 'true');

CREATE POLICY usuario_rol_alta_admin ON authentication.usuario_rol
    FOR INSERT
    WITH CHECK (current_setting('app.is_admin', true) = 'true');

CREATE POLICY usuario_rol_alta_dueno ON authentication.usuario_rol
    FOR INSERT
    TO CURRENT_USER
    WITH CHECK (usuario_id IN (
        SELECT u.id FROM authentication.usuario u
        WHERE u.codigo_usuario = nullif(current_setting('app.current_user', true), '')::uuid
    ));

-- ---------------------------------------------------------------------
-- Permisos de app_runtime
-- ---------------------------------------------------------------------
-- Los usuarios y sus asignaciones no se eliminan.
REVOKE DELETE ON authentication.usuario FROM app_runtime;
REVOKE DELETE ON authentication.usuario_rol FROM app_runtime;

-- El correo no se puede cambiar.
REVOKE UPDATE ON authentication.usuario FROM app_runtime;
GRANT UPDATE (password_hash, estado, invitacion_pendiente, updated_at, deleted_at)
    ON authentication.usuario TO app_runtime;

-- ---------------------------------------------------------------------
-- Cada contraseña la cambia solo su dueño
-- ---------------------------------------------------------------------
-- Una política de fila no distingue columnas y el administrador puede editar
-- cualquier fila: esto le impide cambiar la contraseña de otra persona.
CREATE FUNCTION authentication.exigir_password_propio()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, authentication
AS $$
BEGIN
    IF current_user = 'app_runtime'
        AND nullif(current_setting('app.current_user', true), '')::uuid IS DISTINCT FROM OLD.codigo_usuario THEN
        RAISE EXCEPTION 'Solo el propio usuario puede cambiar su contraseña'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_usuario_password_propio
    BEFORE UPDATE ON authentication.usuario
    FOR EACH ROW
    WHEN (OLD.password_hash IS DISTINCT FROM NEW.password_hash)
    EXECUTE FUNCTION authentication.exigir_password_propio();
