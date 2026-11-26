-- =====================================================================
-- La purga debe funcionar aunque el dueño del schema no sea superusuario
-- (UT-HUI-BITACORA-BE-02). Con FORCE ROW LEVEL SECURITY, un DELETE con WHERE
-- necesita además poder LEER las filas: sin esta política borraba 0 sin avisar.
-- TO CURRENT_USER = el dueño que migra. `app_runtime` no queda incluido aunque
-- active `app.purga`; solo llega a esto a través de la función SECURITY DEFINER.
-- =====================================================================

CREATE POLICY bitacora_purga_lectura ON authentication.bitacora
    FOR SELECT
    TO CURRENT_USER
    USING (current_setting('app.purga', true) = 'on');
