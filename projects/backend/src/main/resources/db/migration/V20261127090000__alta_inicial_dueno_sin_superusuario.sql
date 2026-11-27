-- =====================================================================
-- El alta del administrador inicial debe funcionar aunque el dueño del schema
-- no sea superusuario. Con FORCE ROW LEVEL SECURITY, AdminInicialInitializer
-- contaba 0 administradores y en el segundo arranque intentaba crearlo otra
-- vez: el correo repetido impedía arrancar la aplicación.
-- TO CURRENT_USER = el dueño que migra. `app_runtime` no queda incluido aunque
-- active `app.alta_inicial`.
-- =====================================================================

CREATE POLICY usuario_rol_lectura_alta_inicial ON authentication.usuario_rol
    FOR SELECT
    TO CURRENT_USER
    USING (current_setting('app.alta_inicial', true) = 'on');
