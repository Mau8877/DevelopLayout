package com.example.backend.common.bitacora.model;

import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.backend.common.bitacora.ResultadoBitacora;
import com.example.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Evento de auditoría. Tabla authentication.bitacora: de solo inserción
 * desde la app (sin UPDATE ni DELETE) y de lectura solo para SUPER_ADMIN.
 *
 * * Esta clase es el mapeo de lectura (HU-INICIAL-Bitacora). La escritura
 * * pasa por BitacoraRepository#insertar y BitacoraService, nunca por save().
 */
@Entity
@Table(name = "bitacora", schema = "authentication")
@AttributeOverride(name = "codigo",
        column = @Column(name = "codigo_bitacora", updatable = false, nullable = false, unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bitacora extends BaseModel {

    @Column(name = "usuario_codigo", updatable = false)
    private UUID usuarioCodigo;

    @Column(name = "accion", nullable = false, updatable = false, length = 100)
    private String accion;

    @Column(name = "modulo", nullable = false, updatable = false, length = 50)
    private String modulo;

    @Column(name = "entidad", updatable = false, length = 100)
    private String entidad;

    @Column(name = "entidad_codigo", updatable = false)
    private UUID entidadCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, updatable = false, length = 10)
    private ResultadoBitacora resultado;

    @Column(name = "ip", updatable = false, length = 45)
    private String ip;

    @Column(name = "user_agent", updatable = false, length = 255)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle", updatable = false)
    private Map<String, Object> detalle;
}
