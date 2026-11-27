package com.example.backend.features.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.backend.features.auth.enums.TipoCliente;

/**
 * Sesión que ve su propio dueño. `actual` marca desde cuál se hizo la petición.
 * Sin `id` interno, código de usuario ni hashes del refresh token.
 */
public record MiSesionResponse(UUID codigo, TipoCliente tipoCliente, String ip, String userAgent, Instant creadaEn,
        Instant ultimoUsoEn, Instant expiraEn, boolean actual) {
}
