package com.example.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.example.backend.support.PostgresRuntimeIntegrationTest;
import com.zaxxer.hikari.HikariDataSource;

/** Ninguna espera es indefinida (API4): ni una consulta, ni una conexión, ni Redis. */
@SpringBootTest
// * Un límite corto para poder verlo actuar sin una prueba lenta.
@TestPropertySource(properties = "app.db.statement-timeout-ms=1000")
class EsperasLimitadasTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LettuceConnectionFactory redis;

    @Autowired
    private Flyway flyway;

    @Test
    void unaConsultaQueSeAlarga_laCancelaLaBase() {
        assertThat(jdbc.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("1s");
        assertThatThrownBy(() -> jdbc.execute("SELECT pg_sleep(5)"))
                .rootCause()
                .hasMessageContaining("statement timeout");
    }

    @Test
    void lasMigraciones_noHeredanElLimiteDeConsultas() {
        // * Una migración sobre muchas filas puede tardar más que una consulta de la app.
        JdbcTemplate migraciones = new JdbcTemplate(flyway.getConfiguration().getDataSource());

        assertThat(migraciones.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("0");
    }

    @Test
    void laConexionALaBase_tieneEsperaMaximaYCortaUnaRedMuda() {
        HikariDataSource pool = (HikariDataSource) dataSource;

        assertThat(pool.getConnectionTimeout()).isEqualTo(10_000);
        assertThat(pool.getDataSourceProperties())
                .containsEntry("connectTimeout", "10")
                .containsEntry("socketTimeout", "330")
                .containsEntry("logServerErrorDetail", "false");
    }

    @Test
    void redis_tieneEsperaMaximaPorOrdenYPorConexion() {
        var cliente = redis.getClientConfiguration();

        assertThat(cliente.getCommandTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(cliente.getClientOptions()).hasValueSatisfying(opciones -> assertThat(
                opciones.getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(2)));
    }
}
