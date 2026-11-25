package com.example.backend.common.bitacora.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.example.backend.common.bitacora.model.Bitacora;

/**
 * Repositorio de solo inserción (no extiende JpaRepository a propósito:
 * no expone save, update ni delete).
 *
 * * El INSERT es nativo y sin RETURNING. Con save(), Hibernate emite
 * * `INSERT ... RETURNING id` y Postgres exige que la fila devuelta pase la
 * * política SELECT, que en bitacora es solo para app.is_admin: fallaría para
 * * cualquier usuario normal.
 * * Las consultas las agrega HU-INICIAL-Bitacora.
 */
public interface BitacoraRepository extends Repository<Bitacora, Long> {

    @Modifying
    @Query(value = "INSERT INTO authentication.bitacora "
            + "(codigo_bitacora, usuario_codigo, accion, modulo, entidad, entidad_codigo, "
            + "resultado, ip, user_agent, detalle) "
            + "VALUES (:codigo, CAST(:usuarioCodigo AS uuid), :accion, :modulo, :entidad, "
            + "CAST(:entidadCodigo AS uuid), :resultado, :ip, :userAgent, CAST(:detalle AS jsonb))",
            nativeQuery = true)
    void insertar(
            @Param("codigo") UUID codigo,
            @Param("usuarioCodigo") UUID usuarioCodigo,
            @Param("accion") String accion,
            @Param("modulo") String modulo,
            @Param("entidad") String entidad,
            @Param("entidadCodigo") UUID entidadCodigo,
            @Param("resultado") String resultado,
            @Param("ip") String ip,
            @Param("userAgent") String userAgent,
            @Param("detalle") String detalleJson);
}
