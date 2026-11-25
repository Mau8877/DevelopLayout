-- =====================================================================
-- Rol de runtime de la aplicación (UT-HUI-BASE-BE-02).
-- Sin SUPERUSER ni BYPASSRLS: sin eso, RLS no protege nada. Flyway corre con
-- el dueño del schema (SPRING_FLYWAY_USER); la app, con este rol.
--
-- ! La contraseña llega por el placeholder de Flyway ${APP_RUNTIME_PASSWORD}
-- ! (variable de entorno). Debe ser alfanumérica: se escribe entre comillas
-- ! simples. Cambiarla después NO se hace editando esta migración (ya aplicada):
-- ! ALTER ROLE app_runtime PASSWORD '...' a mano.
-- =====================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_runtime') THEN
        CREATE ROLE app_runtime LOGIN PASSWORD '${APP_RUNTIME_PASSWORD}'
            NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT USAGE ON SCHEMA authentication TO app_runtime;

GRANT SELECT, INSERT, UPDATE, DELETE ON authentication.usuario TO app_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON authentication.usuario_rol TO app_runtime;
GRANT SELECT ON authentication.rol TO app_runtime;
-- Las sesiones no se borran: se marcan REVOCADA o EXPIRADA.
GRANT SELECT, INSERT, UPDATE ON authentication.sesion TO app_runtime;
-- La bitácora es de solo inserción y lectura; la purga pasa por la función.
GRANT SELECT, INSERT ON authentication.bitacora TO app_runtime;

GRANT USAGE ON ALL SEQUENCES IN SCHEMA authentication TO app_runtime;
GRANT EXECUTE ON FUNCTION authentication.purgar_bitacora(integer) TO app_runtime;

-- Tablas futuras del schema: lectura e inserción por defecto. UPDATE y DELETE
-- se conceden tabla por tabla, en la migración que las crea.
ALTER DEFAULT PRIVILEGES IN SCHEMA authentication
    GRANT SELECT, INSERT ON TABLES TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA authentication
    GRANT USAGE, SELECT ON SEQUENCES TO app_runtime;
