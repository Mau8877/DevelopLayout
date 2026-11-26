package com.example.backend.features.bitacora;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * En una base administrada el dueño del esquema no suele ser superusuario y,
 * con FORCE ROW LEVEL SECURITY, las políticas también lo limitan. La purga
 * tiene que borrar igual, sin abrirle nada a `app_runtime`.
 */
@Testcontainers(disabledWithoutDocker = true)
class PurgaDuenoSinSuperusuarioTest {

    private static final String CLAVE = "duenoDescartable1";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static JdbcTemplate conectar(String usuario, String clave, String base) {
        String url = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + base);
        return new JdbcTemplate(new DriverManagerDataSource(url, usuario, clave));
    }

    @Test
    void purga_conDuenoSinSuperusuario_borraSoloLoAntiguoSinAbrirNadaAlRuntime() {
        JdbcTemplate superusuario = conectar(POSTGRES.getUsername(), POSTGRES.getPassword(),
                POSTGRES.getDatabaseName());
        superusuario.execute("CREATE ROLE dueno_comun LOGIN CREATEROLE NOSUPERUSER NOBYPASSRLS PASSWORD '"
                + CLAVE + "'");
        superusuario.execute("CREATE DATABASE base_comun OWNER dueno_comun");
        String url = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/base_comun");
        Flyway.configure()
                .dataSource(url, "dueno_comun", CLAVE)
                .locations("classpath:db/migration")
                .placeholders(Map.of("APP_RUNTIME_PASSWORD", CLAVE))
                .load()
                .migrate();
        JdbcTemplate admin = conectar(POSTGRES.getUsername(), POSTGRES.getPassword(), "base_comun");
        admin.update("INSERT INTO authentication.bitacora (codigo_bitacora, accion, modulo, resultado, created_at) "
                + "VALUES (gen_random_uuid(), 'VIEJO', 'auth', 'EXITO', now() - interval '13 months'), "
                + "(gen_random_uuid(), 'NUEVO', 'auth', 'EXITO', now() - interval '2 months')");
        JdbcTemplate runtime = conectar("app_runtime", CLAVE, "base_comun");

        // Activar el contexto de purga a mano no le da lectura al rol de la app.
        String lectura = runtime.execute((org.springframework.jdbc.core.ConnectionCallback<String>) conexion -> {
            conexion.setAutoCommit(false);
            try (var sentencia = conexion.createStatement()) {
                sentencia.execute("SELECT set_config('app.purga', 'on', true)");
                var filas = sentencia.executeQuery("SELECT count(*) FROM authentication.bitacora");
                filas.next();
                return filas.getString(1);
            } finally {
                conexion.rollback();
            }
        });
        assertThat(lectura).isEqualTo("0");

        Long borradas = runtime.queryForObject("SELECT authentication.purgar_bitacora(12)", Long.class);

        assertThat(borradas).isEqualTo(1L);
        assertThat(admin.queryForList("SELECT accion FROM authentication.bitacora", String.class))
                .containsExactly("NUEVO");
    }
}
