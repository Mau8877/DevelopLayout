package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La migración a tokens firmados expira las sesiones abiertas también cuando el
 * dueño del schema no es superusuario (con RLS forzado vería 0 filas).
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionRefreshFirmadoTest {

    private static final String CLAVE = "duenoDescartable1";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static JdbcTemplate conectar(String base) {
        return new JdbcTemplate(new DriverManagerDataSource(url(base), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    private static String url(String base) {
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + base);
    }

    private static Flyway flyway(String hasta) {
        return Flyway.configure()
                .dataSource(url("base_migracion"), "dueno_migracion", CLAVE)
                .locations("classpath:db/migration")
                .placeholders(Map.of("APP_RUNTIME_PASSWORD", CLAVE))
                .target(hasta)
                .load();
    }

    @Test
    void migracion_expiraLasSesionesAbiertasYDejaRlsForzado() {
        JdbcTemplate raiz = conectar(POSTGRES.getDatabaseName());
        raiz.execute("CREATE ROLE dueno_migracion LOGIN CREATEROLE NOSUPERUSER NOBYPASSRLS PASSWORD '" + CLAVE + "'");
        raiz.execute("CREATE DATABASE base_migracion OWNER dueno_migracion");
        flyway("20261126090100").migrate();
        JdbcTemplate admin = conectar("base_migracion");
        admin.update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (gen_random_uuid(), 'a@test.local', 'x', 'ACTIVO')");
        admin.update("INSERT INTO authentication.sesion (codigo_sesion, usuario_id, usuario_codigo, refresh_hash, "
                + "estado, tipo_cliente, expira_en) SELECT gen_random_uuid(), id, codigo_usuario, repeat('a', 64), "
                + "'ACTIVA', 'WEB', now() + interval '7 days' FROM authentication.usuario");

        flyway("latest").migrate();

        assertThat(admin.queryForObject("SELECT estado FROM authentication.sesion", String.class))
                .isEqualTo("EXPIRADA");
        assertThat(admin.queryForObject("SELECT relforcerowsecurity FROM pg_class "
                + "WHERE oid = 'authentication.sesion'::regclass", Boolean.class)).isTrue();
        assertThat(admin.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema = "
                + "'authentication' AND column_name = 'refresh_hash_anterior'", Integer.class)).isZero();
    }
}
