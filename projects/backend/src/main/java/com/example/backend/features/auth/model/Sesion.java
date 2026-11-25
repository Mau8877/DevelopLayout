package com.example.backend.features.auth.model;

import java.time.Instant;
import java.util.UUID;

import com.example.backend.common.model.BaseModel;
import com.example.backend.features.auth.enums.EstadoSesion;
import com.example.backend.features.auth.enums.MotivoRevocacion;
import com.example.backend.features.auth.enums.TipoCliente;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sesión de un cliente (web o mobile). Tabla authentication.sesion (RLS
 * forzado); es la fuente de verdad de las sesiones.
 *
 * * `usuarioId` y `usuarioCodigo` son columnas simples y no un @ManyToOne:
 * * la FK compuesta (usuario_id, usuario_codigo) garantiza que coincidan, y
 * * `usuarioCodigo` existe para que la política de RLS compare sin subconsulta.
 * ! Nunca se guarda el refresh token en claro, solo su SHA-256 hexadecimal;
 * ! los hashes no entran en ningún toString (esta clase no define toString).
 */
@Entity
@Table(name = "sesion", schema = "authentication")
@AttributeOverride(name = "codigo",
        column = @Column(name = "codigo_sesion", updatable = false, nullable = false, unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sesion extends BaseModel {

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "usuario_codigo", nullable = false, updatable = false)
    private UUID usuarioCodigo;

    @Setter
    @Column(name = "refresh_hash", nullable = false, length = 64)
    private String refreshHash;

    @Setter
    @Column(name = "refresh_hash_anterior", length = 64)
    private String refreshHashAnterior;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSesion estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente", nullable = false, updatable = false, length = 10)
    private TipoCliente tipoCliente;

    @Column(name = "ip", updatable = false, length = 45)
    private String ip;

    @Column(name = "user_agent", updatable = false, length = 255)
    private String userAgent;

    @Setter
    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Setter
    @Column(name = "ultimo_uso_en")
    private Instant ultimoUsoEn;

    @Setter
    @Column(name = "revocada_en")
    private Instant revocadaEn;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_revocacion", length = 30)
    private MotivoRevocacion motivoRevocacion;

    public Sesion(Long usuarioId, UUID usuarioCodigo, String refreshHash, TipoCliente tipoCliente,
            String ip, String userAgent, Instant expiraEn) {
        this.usuarioId = usuarioId;
        this.usuarioCodigo = usuarioCodigo;
        this.refreshHash = refreshHash;
        this.tipoCliente = tipoCliente;
        this.ip = ip;
        this.userAgent = userAgent;
        this.expiraEn = expiraEn;
        this.estado = EstadoSesion.ACTIVA;
    }
}
