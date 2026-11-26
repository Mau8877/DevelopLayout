package com.example.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Redis guarda datos de seguridad (bloqueos de login y sesiones revocadas): no
 * puede desalojarlos al llenarse. Con TTL, las políticas `volatile-*` también
 * los borrarían, así que solo sirve `noeviction`.
 */
class RedisConfigTest {

    @Test
    void redisConf_noDesalojaClaves() throws IOException {
        assertThat(Files.readAllLines(Path.of("../../infrastructure/redis/redis.conf")))
                .filteredOn(linea -> linea.startsWith("maxmemory-policy"))
                .containsExactly("maxmemory-policy noeviction");
    }
}
