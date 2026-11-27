package com.example.backend.features.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.backend.features.auth.enums.TipoCliente;

/** Sesión que ve el administrador. Sin `id` interno ni hashes del refresh token. */
public record SesionResponse(UUID codigo, UUID usuarioCodigo, TipoCliente tipoCliente, String ip, String userAgent,
        Instant creadaEn, Instant ultimoUsoEn, Instant expiraEn) {
}
