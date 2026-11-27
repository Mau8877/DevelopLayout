package com.example.backend.features.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * En una base administrada el dueño del esquema no suele ser superusuario y,
 * con FORCE ROW LEVEL SECURITY, las políticas también lo limitan. El segundo
 * arranque tiene que reconocer al administrador que creó el primero.
 */
@Testcontainers(disabledWithoutDocker = true)
class AdminInicialDuenoSinSuperusuarioTest {

    private static final String CLAVE = "duenoDescartable1"; // gitleaks:allow

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static String url(String base) {
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + base);
    }

    private static JdbcTemplate conectar(String usuario, String clave, String base) {
        return new JdbcTemplate(new DriverManagerDataSource(url(base), usuario, clave));
    }

    private static AdminInicialInitializer arranque(String email, String password) {
        return new AdminInicialInitializer(url("base_admin"), "dueno_admin", CLAVE, email, password);
    }

    @BeforeAll
    static void migrarConDuenoSinSuperusuario() {
        JdbcTemplate superusuario = conectar(POSTGRES.getUsername(), POSTGRES.getPassword(),
                POSTGRES.getDatabaseName());
        superusuario.execute("CREATE ROLE dueno_admin LOGIN CREATEROLE NOSUPERUSER NOBYPASSRLS PASSWORD '"
                + CLAVE + "'");
        superusuario.execute("CREATE DATABASE base_admin OWNER dueno_admin");
        Flyway.configure()
                .dataSource(url("base_admin"), "dueno_admin", CLAVE)
                .locations("classpath:db/migration")
                .placeholders(Map.of("APP_RUNTIME_PASSWORD", CLAVE))
                .load()
                .migrate();
    }

    @Test
    void segundoArranque_conDuenoSinSuperusuario_reconoceAlAdministradorExistente() {
        arranque("admin@test.local", "ClaveValida123").run(null); // gitleaks:allow

        assertThatCode(() -> arranque("admin@test.local", "ClaveValida123").run(null)) // gitleaks:allow
                .doesNotThrowAnyException();
        // Con un administrador ya creado, las variables INITIAL_ADMIN_* dejan de ser necesarias.
        assertThatCode(() -> arranque("", "").run(null)).doesNotThrowAnyException();
        assertThatCode(() -> arranque("otro@test.local", "ClaveValida123").run(null)) // gitleaks:allow
                .doesNotThrowAnyException();

        JdbcTemplate verificacion = conectar(POSTGRES.getUsername(), POSTGRES.getPassword(), "base_admin");
        assertThat(verificacion.queryForList("SELECT email FROM authentication.usuario", String.class))
                .containsExactly("admin@test.local");
        assertThat(verificacion.queryForObject("SELECT count(*) FROM authentication.usuario_rol", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void contextoDeAltaInicial_activadoPorAppRuntime_noDaLecturaDeRoles() {
        arranque("admin@test.local", "ClaveValida123").run(null); // gitleaks:allow
        JdbcTemplate runtime = conectar("app_runtime", CLAVE, "base_admin");

        String lectura = runtime.execute((ConnectionCallback<String>) conexion -> {
            conexion.setAutoCommit(false);
            try (var sentencia = conexion.createStatement()) {
                sentencia.execute("SELECT set_config('app.alta_inicial', 'on', true)");
                var filas = sentencia.executeQuery("SELECT count(*) FROM authentication.usuario_rol");
                filas.next();
                return filas.getString(1);
            } finally {
                conexion.rollback();
            }
        });

        assertThat(lectura).isEqualTo("0");
    }
}
