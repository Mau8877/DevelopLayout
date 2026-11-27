package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Prueba que RLS filtra de verdad y falla cerrado, conectándose como
 * `app_runtime` (no como el dueño, que es superusuario e ignora RLS).
 *
 * * Aplica las migraciones reales con Flyway sobre un Postgres descartable.
 * * El ciclo de vida del container es manual para saltear el test (no romper
 * * el build) si Docker no está disponible, ver TESTING_BACKEND.md.
 */
class RlsAutenticacionTest {

    private static final String PASSWORD_RUNTIME = "runtimepass123";
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    private static PostgreSQLContainer postgres;
    private static final UUID CODIGO_A = UUID.randomUUID();
    private static final UUID CODIGO_B = UUID.randomUUID();
    private static long idA;
    private static long idB;

    @BeforeAll
    static void prepararBase() throws Exception {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta en vez de romper el build.");

        postgres = new PostgreSQLContainer("postgres:18-alpine");
        postgres.start();

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .placeholders(Map.of("APP_RUNTIME_PASSWORD", PASSWORD_RUNTIME))
                .load()
                .migrate();

        try (Connection owner = dueno(); Statement st = owner.createStatement()) {
            idA = insertarUsuario(owner, CODIGO_A, "a@test.local");
            idB = insertarUsuario(owner, CODIGO_B, "b@test.local");
            st.execute("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                    + "SELECT gen_random_uuid(), " + idA + ", id FROM authentication.rol WHERE nombre = 'USUARIO'");
            st.execute("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                    + "SELECT gen_random_uuid(), " + idB + ", id FROM authentication.rol WHERE nombre = 'USUARIO'");
            insertarSesion(owner, idA, CODIGO_A, HASH_A);
            insertarSesion(owner, idB, CODIGO_B, HASH_B);
        }
    }

    @AfterAll
    static void apagarBase() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // ------------------------------------------------------------------ usuario

    @Test
    void sinContexto_noSeVeNingunaFilaDeUsuarioNiDeUsuarioRol() throws Exception {
        assertThat(contar("authentication.usuario", Map.of())).isZero();
        assertThat(contar("authentication.usuario_rol", Map.of())).isZero();
    }

    @Test
    void despuesDeUsarUnContexto_laSiguienteTransaccionSinContextoSigueFallandoCerrado() throws Exception {
        try (Connection c = runtime()) {
            assertThat(contar(c, "authentication.usuario", Map.of("app.current_user", CODIGO_A.toString()))).isEqualTo(1);
            // El GUC queda como '' (no NULL) en la conexión: no debe romper ni abrir filas.
            assertThat(contar(c, "authentication.usuario", Map.of())).isZero();
        }
    }

    @Test
    void conCurrentUser_soloSeVeElPropioUsuarioYSusRoles() throws Exception {
        Map<String, String> ctx = Map.of("app.current_user", CODIGO_A.toString());
        assertThat(contar("authentication.usuario", ctx)).isEqualTo(1);
        assertThat(contar("authentication.usuario_rol", ctx)).isEqualTo(1);
    }

    @Test
    void conLoginEmail_soloSeVeEsaFilaIgnorandoMayusculas() throws Exception {
        assertThat(contar("authentication.usuario", Map.of("app.login_email", "A@TEST.LOCAL"))).isEqualTo(1);
    }

    @Test
    void actualizarLaFilaDeOtroUsuario_noAfectaFilas() throws Exception {
        try (Connection c = runtime()) {
            fijarContexto(c, Map.of("app.current_user", CODIGO_A.toString()));
            try (Statement st = c.createStatement()) {
                int filas = st.executeUpdate(
                        "UPDATE authentication.usuario SET estado = 'INACTIVO' WHERE email = 'b@test.local'");
                assertThat(filas).isZero();
            }
            c.rollback();
        }
    }

    @Test
    void appRuntime_noPuedeInsertarEnRol() throws Exception {
        assertThat(sqlState(() -> ejecutarRuntime(
                "INSERT INTO authentication.rol (codigo_rol, nombre) VALUES (gen_random_uuid(), 'HACKER')")))
                .isEqualTo("42501");
    }

    @Test
    void appRuntime_puedeLeerElCatalogoDeRoles() throws Exception {
        assertThat(contar("authentication.rol", Map.of())).isEqualTo(2);
    }

    // ------------------------------------------------------------------- sesion

    @Test
    void sesion_sinContexto_noSeVeNada() throws Exception {
        assertThat(contar("authentication.sesion", Map.of())).isZero();
    }

    @Test
    void sesion_conCurrentUser_soloSeVenLasPropias() throws Exception {
        assertThat(contar("authentication.sesion", Map.of("app.current_user", CODIGO_A.toString()))).isEqualTo(1);
    }

    @Test
    void sesion_conRefreshSid_soloSeVeEsaSesion() throws Exception {
        String codigoSesion;
        try (Connection owner = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword());
                PreparedStatement ps = owner.prepareStatement(
                        "SELECT codigo_sesion::text FROM authentication.sesion WHERE refresh_hash = ?")) {
            ps.setString(1, HASH_B);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                codigoSesion = rs.getString(1);
            }
        }

        assertThat(contar("authentication.sesion", Map.of("app.refresh_sid", codigoSesion))).isEqualTo(1);
        assertThat(contar("authentication.sesion", Map.of("app.refresh_sid", UUID.randomUUID().toString())))
                .isZero();
        // El contexto anterior por hash ya no abre nada.
        assertThat(contar("authentication.sesion", Map.of("app.refresh_hash", HASH_B))).isZero();
    }

    @Test
    void sesion_conIsAdmin_seVenTodas() throws Exception {
        assertThat(contar("authentication.sesion", Map.of("app.is_admin", "true"))).isEqualTo(2);
    }

    @Test
    void sesion_appRuntimeNoPuedeBorrar() {
        assertThat(sqlState(() -> ejecutarRuntime("DELETE FROM authentication.sesion"))).isEqualTo("42501");
    }

    @Test
    void sesion_conUsuarioCodigoDeOtroUsuario_fallaPorLaFkCompuesta() throws Exception {
        try (Connection owner = dueno()) {
            assertThat(sqlState(() -> insertarSesion(owner, idA, CODIGO_B, "d".repeat(64))))
                    .isEqualTo("23503");
        }
    }

    // ----------------------------------------------------------------- bitacora

    @Test
    void bitacora_insertSinContextoFunciona_perSelectSoloConIsAdmin() throws Exception {
        try (Connection c = runtime()) {
            fijarContexto(c, Map.of());
            try (Statement st = c.createStatement()) {
                st.executeUpdate("INSERT INTO authentication.bitacora "
                        + "(codigo_bitacora, accion, modulo, resultado) "
                        + "VALUES (gen_random_uuid(), 'LOGIN_FALLIDO', 'auth', 'FALLO')");
            }
            c.commit();
        }
        assertThat(contar("authentication.bitacora", Map.of())).isZero();
        assertThat(contar("authentication.bitacora", Map.of("app.is_admin", "true"))).isGreaterThanOrEqualTo(1);
    }

    @Test
    void bitacora_updateYDelete_seRechazan() {
        assertThat(sqlState(() -> ejecutarRuntime("UPDATE authentication.bitacora SET accion = 'x'")))
                .isEqualTo("42501");
        assertThat(sqlState(() -> ejecutarRuntime("DELETE FROM authentication.bitacora"))).isEqualTo("42501");
    }

    @Test
    void purgarBitacora_borraSoloLoAnteriorAlPlazo() throws Exception {
        try (Connection owner = dueno(); Statement st = owner.createStatement()) {
            st.execute("DELETE FROM authentication.bitacora");
            st.execute("INSERT INTO authentication.bitacora (codigo_bitacora, accion, modulo, resultado, created_at) "
                    + "VALUES (gen_random_uuid(), 'VIEJO', 'auth', 'EXITO', now() - interval '13 months')");
            st.execute("INSERT INTO authentication.bitacora (codigo_bitacora, accion, modulo, resultado) "
                    + "VALUES (gen_random_uuid(), 'RECIENTE', 'auth', 'EXITO')");
        }

        long borradas;
        try (Connection c = runtime(); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT authentication.purgar_bitacora(12)")) {
            rs.next();
            borradas = rs.getLong(1);
            c.commit();
        }

        assertThat(borradas).isEqualTo(1);
        assertThat(contar("authentication.bitacora", Map.of("app.is_admin", "true"))).isEqualTo(1);
    }

    // ------------------------------------------------------------------- schema

    @Test
    void appRuntime_noPuedeCrearObjetosEnAuthenticationNiEnPublic() {
        assertThat(sqlState(() -> ejecutarRuntime("CREATE TABLE authentication.x (id int)"))).isEqualTo("42501");
        assertThat(sqlState(() -> ejecutarRuntime("CREATE TABLE public.x (id int)"))).isEqualTo("42501");
    }

    @Test
    void appRuntime_noEsSuperusuarioNiIgnoraRls() throws Exception {
        try (Connection owner = dueno(); Statement st = owner.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT rolsuper, rolbypassrls FROM pg_roles WHERE rolname = 'app_runtime'")) {
            rs.next();
            assertThat(rs.getBoolean(1)).isFalse();
            assertThat(rs.getBoolean(2)).isFalse();
        }
    }

    @Test
    void todasLasTablasDeAuthenticationConDatosTienenRlsForzado() throws Exception {
        try (Connection owner = dueno(); Statement st = owner.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT relname, relrowsecurity, relforcerowsecurity FROM pg_class "
                                + "WHERE relnamespace = 'authentication'::regnamespace AND relkind = 'r' "
                                + "AND relname <> 'rol'")) {
            int tablas = 0;
            while (rs.next()) {
                tablas++;
                assertThat(rs.getBoolean(2)).as(rs.getString(1) + " ENABLE").isTrue();
                assertThat(rs.getBoolean(3)).as(rs.getString(1) + " FORCE").isTrue();
            }
            assertThat(tablas).isEqualTo(4);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Connection dueno() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static Connection runtime() throws SQLException {
        Connection c = DriverManager.getConnection(postgres.getJdbcUrl(), "app_runtime", PASSWORD_RUNTIME);
        c.setAutoCommit(false);
        return c;
    }

    private static long insertarUsuario(Connection owner, UUID codigo, String email) throws SQLException {
        try (PreparedStatement ps = owner.prepareStatement(
                "INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                        + "VALUES (?, ?, 'hash', 'ACTIVO') RETURNING id")) {
            ps.setObject(1, codigo);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void insertarSesion(Connection owner, long usuarioId, UUID usuarioCodigo, String hash)
            throws SQLException {
        try (PreparedStatement ps = owner.prepareStatement(
                "INSERT INTO authentication.sesion (codigo_sesion, usuario_id, usuario_codigo, refresh_hash, "
                        + "estado, tipo_cliente, expira_en) "
                        + "VALUES (gen_random_uuid(), ?, ?, ?, 'ACTIVA', 'WEB', now() + interval '7 days')")) {
            ps.setLong(1, usuarioId);
            ps.setObject(2, usuarioCodigo);
            ps.setString(3, hash);
            ps.executeUpdate();
        }
    }

    private static void fijarContexto(Connection c, Map<String, String> contexto) throws SQLException {
        for (Map.Entry<String, String> e : new LinkedHashMap<>(contexto).entrySet()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT set_config(?, ?, true)")) {
                ps.setString(1, e.getKey());
                ps.setString(2, e.getValue());
                ps.execute();
            }
        }
    }

    private static int contar(String tabla, Map<String, String> contexto) throws SQLException {
        try (Connection c = runtime()) {
            return contar(c, tabla, contexto);
        }
    }

    private static int contar(Connection c, String tabla, Map<String, String> contexto) throws SQLException {
        fijarContexto(c, contexto);
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM " + tabla)) {
            rs.next();
            int total = rs.getInt(1);
            c.rollback();
            return total;
        }
    }

    private static void ejecutarRuntime(String sql) throws SQLException {
        try (Connection c = runtime(); Statement st = c.createStatement()) {
            st.execute(sql);
            c.commit();
        }
    }

    @FunctionalInterface
    private interface AccionSql {
        void ejecutar() throws SQLException;
    }

    private static String sqlState(AccionSql accion) {
        Throwable error = catchThrowable(accion);
        assertThat(error).as("se esperaba un error de base de datos").isInstanceOf(SQLException.class);
        return ((SQLException) error).getSQLState();
    }

    private static Throwable catchThrowable(AccionSql accion) {
        try {
            accion.ejecutar();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
