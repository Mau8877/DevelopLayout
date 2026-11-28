package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** El motivo `CAMBIO_PASSWORD` se agrega sobre una base que ya tiene sesiones cerradas. */
@Testcontainers(disabledWithoutDocker = true)
class MigracionCambioPasswordTest {

    private static final String CLAVE = "duenoDescartable1";
    private static final List<String> ANTERIORES = List.of("LOGOUT", "USUARIO", "ADMIN", "REUSO", "RESET_PASSWORD");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static JdbcTemplate conectar() {
        return new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    private static Flyway flyway(String hasta) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .placeholders(Map.of("APP_RUNTIME_PASSWORD", CLAVE))
                .target(hasta)
                .load();
    }

    private static void insertarSesion(JdbcTemplate base, String motivo, int numero) {
        base.update("INSERT INTO authentication.sesion (codigo_sesion, usuario_id, usuario_codigo, refresh_hash, "
                + "estado, tipo_cliente, expira_en, motivo_revocacion) SELECT gen_random_uuid(), id, codigo_usuario, "
                + "repeat(?, 64), 'REVOCADA', 'WEB', now() + interval '7 days', ? FROM authentication.usuario",
                String.valueOf(numero), motivo);
    }

    @Test
    void migracion_conservaLosMotivosAnteriores_aceptaElNuevoYRechazaOtros() {
        flyway("20261127110000").migrate();
        JdbcTemplate base = conectar();
        base.update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (gen_random_uuid(), 'a@test.local', 'x', 'ACTIVO')");
        for (int i = 0; i < ANTERIORES.size(); i++) {
            insertarSesion(base, ANTERIORES.get(i), i);
        }
        assertThatThrownBy(() -> insertarSesion(base, "CAMBIO_PASSWORD", 7))
                .isInstanceOf(DataIntegrityViolationException.class);

        flyway("latest").migrate();

        insertarSesion(base, "CAMBIO_PASSWORD", 7);
        assertThat(base.queryForList("SELECT motivo_revocacion FROM authentication.sesion", String.class))
                .containsExactlyInAnyOrder("LOGOUT", "USUARIO", "ADMIN", "REUSO", "RESET_PASSWORD",
                        "CAMBIO_PASSWORD");
        assertThatThrownBy(() -> insertarSesion(base, "INVENTADO", 8))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(base.queryForObject("SELECT relforcerowsecurity FROM pg_class "
                + "WHERE oid = 'authentication.sesion'::regclass", Boolean.class)).isTrue();
    }
}
