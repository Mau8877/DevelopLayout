package com.example.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.exception.GlobalExceptionHandler;
import com.example.backend.support.PostgresRuntimeIntegrationTest;

import jakarta.persistence.EntityManager;

/**
 * Un error inesperado de la base no deja datos personales en el log, y el log
 * sigue sirviendo para encontrar la causa.
 *
 * * Postgres repite los valores en sus mensajes: en el principal (un valor mal
 * * formado) y en el detalle (una clave repetida).
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class DatosEnErroresInesperadosTest extends PostgresRuntimeIntegrationTest {

    private static final String CORREO = "persona.sonda@test.local";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaccion;

    @Autowired
    private GlobalExceptionHandler manejador;

    @Test
    void valorRepetido_noEscribeElValor(CapturedOutput salida) {
        RuntimeException error = fallar(() -> {
            sql("CREATE TEMP TABLE sonda (correo text CONSTRAINT sonda_correo_unico UNIQUE) ON COMMIT DROP");
            insertar();
            insertar();
        });

        manejador.handleUnexpectedException(error);

        assertThat(salida.getAll()).doesNotContain(CORREO);
        // Lo que permite diagnosticar: qué falló, el código de Postgres, la restricción y dónde.
        assertThat(salida.getAll()).contains("Error interno no controlado", "ConstraintViolationException",
                "sqlState=23505", "sonda_correo_unico", "DatosEnErroresInesperadosTest");
    }

    @Test
    void valorMalFormado_noEscribeElValor(CapturedOutput salida) {
        RuntimeException error = fallar(() -> entityManager
                .createNativeQuery("SELECT CAST(:valor AS uuid)")
                .setParameter("valor", CORREO)
                .getSingleResult());

        manejador.handleUnexpectedException(error);

        assertThat(salida.getAll()).doesNotContain(CORREO);
        assertThat(salida.getAll()).contains("Error interno no controlado", "sqlState=22P02");
    }

    private RuntimeException fallar(Runnable operacion) {
        return catchThrowableOfType(RuntimeException.class,
                () -> transaccion.executeWithoutResult(estado -> operacion.run()));
    }

    private void sql(String sentencia) {
        entityManager.createNativeQuery(sentencia).executeUpdate();
    }

    private void insertar() {
        entityManager.createNativeQuery("INSERT INTO sonda (correo) VALUES (:correo)")
                .setParameter("correo", CORREO)
                .executeUpdate();
    }
}
