-- Un motivo más de cierre de sesión: el cambio de contraseña con sesión iniciada.
-- Solo cambia la restricción: no modifica datos, permisos ni políticas de RLS.
ALTER TABLE authentication.sesion
    DROP CONSTRAINT sesion_motivo_revocacion_check;

ALTER TABLE authentication.sesion
    ADD CONSTRAINT sesion_motivo_revocacion_check
        CHECK (motivo_revocacion IN ('LOGOUT', 'USUARIO', 'ADMIN', 'REUSO', 'RESET_PASSWORD', 'CAMBIO_PASSWORD'));
