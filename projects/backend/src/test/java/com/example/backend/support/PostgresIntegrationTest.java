package com.example.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de los tests que cargan el contexto completo de Spring: levanta un
 * Postgres real y descartable con Testcontainers, en vez de depender de un
 * Postgres instalado en la máquina (o de credenciales del entorno).
 *
 * * Un solo contenedor compartido por toda la JVM de tests (se arranca la
 * * primera vez que se necesita y lo apaga Ryuk al terminar).
 * * Si Docker no está disponible, los tests que heredan de esta clase
 * * quedan SKIPPED (no rompen el build), igual que TestcontainersSmokeTest.
 * * Flyway usa el mismo usuario que la app porque en test hay un solo rol.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @DynamicPropertySource
    static void configurarBaseDeDatos(DynamicPropertyRegistry registry) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
