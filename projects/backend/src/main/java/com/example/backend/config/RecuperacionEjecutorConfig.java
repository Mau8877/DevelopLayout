package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Hilos donde se procesa la recuperación de contraseña, fuera de la petición.
 *
 * * Acotado a propósito (API4): con la cola llena rechaza en vez de acumular.
 * * No es persistente: al apagar espera unos segundos los envíos pendientes; lo
 * * que no alcance se pierde y el usuario pide otro enlace.
 * ! `defaultCandidate = false`: sin esto Spring Boot dejaría de crear su
 * ! ejecutor por defecto y otras tareas asíncronas caerían en esta cola chica.
 */
@Configuration
public class RecuperacionEjecutorConfig {

    public static final String EJECUTOR = "recuperacionEjecutor";

    @Bean(name = EJECUTOR, defaultCandidate = false)
    public ThreadPoolTaskExecutor recuperacionEjecutor() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(2);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("recuperacion-");
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(10);
        return ejecutor;
    }
}
