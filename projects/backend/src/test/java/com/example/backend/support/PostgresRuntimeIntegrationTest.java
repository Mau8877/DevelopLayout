package com.example.backend.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Igual que PostgresIntegrationTest (más un Redis descartable), pero la app corre como `app_runtime`
 * (sin superusuario, con RLS aplicando de verdad), como en producción.
 *
 * * Flyway usa el dueño del esquema; las preparaciones y verificaciones de
 * * datos usan {@link #dueno()}, que ignora RLS.
 * * Si Docker no está disponible, los tests quedan SKIPPED.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresRuntimeIntegrationTest {

    protected static final String PASSWORD_RUNTIME = "runtimepass123";
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configurarBaseDeDatos(DynamicPropertyRegistry registry) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "app_runtime");
        registry.add("spring.datasource.password", () -> PASSWORD_RUNTIME);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.flyway.placeholders.APP_RUNTIME_PASSWORD", () -> PASSWORD_RUNTIME);
    }

    /** Conexión del dueño del esquema (ignora RLS), solo para preparar y verificar datos. */
    protected static JdbcTemplate dueno() {
        return new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }
}
