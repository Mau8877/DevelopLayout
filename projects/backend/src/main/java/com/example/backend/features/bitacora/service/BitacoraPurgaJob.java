package com.example.backend.features.bitacora.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.exception.ExcepcionSinMensajes;

/**
 * Borra cada día los eventos anteriores al período de retención.
 *
 * * Pasa por la función `purgar_bitacora` (SECURITY DEFINER): la app no tiene
 * * DELETE sobre la tabla. El resultado va a los logs, no a la bitácora.
 * ! Pensado para una sola instancia: con más réplicas hace falta un bloqueo
 * ! distribuido para que no corra varias veces.
 */
@Component
public class BitacoraPurgaJob {

    private static final Logger LOG = LoggerFactory.getLogger(BitacoraPurgaJob.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final int meses;

    public BitacoraPurgaJob(JdbcTemplate jdbc, TransactionTemplate transaccion,
            @Value("${app.bitacora.retention-months:12}") int meses) {
        if (meses < 1) {
            throw new IllegalStateException("app.bitacora.retention-months debe ser 1 o más");
        }
        this.jdbc = jdbc;
        this.transaccion = transaccion;
        this.meses = meses;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgar() {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "bitacora")) {
            try {
                Long borradas = transaccion.execute(estado -> {
                    // * Con muchos eventos pendientes tarda más que una consulta normal. Solo en esta transacción.
                    jdbc.execute("SET LOCAL statement_timeout = '5min'");
                    return jdbc.queryForObject("SELECT authentication.purgar_bitacora(?)", Long.class, meses);
                });
                LOG.info("Purga de bitácora: eventos borrados={} retencionMeses={}", borradas, meses);
            } catch (RuntimeException e) {
                LOG.error("Falló la purga de la bitácora", ExcepcionSinMensajes.de(e));
            }
        }
    }
}
