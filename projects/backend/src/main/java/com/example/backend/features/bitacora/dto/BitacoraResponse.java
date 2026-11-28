package com.example.backend.features.bitacora.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.example.backend.common.bitacora.enums.ResultadoBitacora;

/** Evento de auditoría, sin el `id` interno. */
public record BitacoraResponse(UUID codigo, Instant fecha, UUID usuarioCodigo, String accion, String modulo,
        String entidad, UUID entidadCodigo, ResultadoBitacora resultado, String ip, Map<String, Object> detalle) {
}
