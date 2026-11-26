package com.example.backend.features.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.features.auth.enums.EstadoSesion;
import com.example.backend.features.auth.model.Sesion;

import jakarta.persistence.LockModeType;

/**
 * ! Con RLS forzado, solo se ven las sesiones del `app.current_user`, las de
 * ! `app.is_admin` o la de `app.refresh_sid` (token con firma ya validada).
 */
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByRefreshHash(String refreshHash);

    Optional<Sesion> findByCodigo(UUID codigo);

    /** Sesiones utilizables del usuario: `ACTIVA` y no vencidas. */
    Page<Sesion> findByUsuarioCodigoAndEstadoAndExpiraEnAfter(UUID usuarioCodigo, EstadoSesion estado,
            Instant despuesDe, Pageable pageable);

    /** Lee la sesión bloqueando la fila hasta el fin de la transacción. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sesion s where s.codigo = :codigo")
    Optional<Sesion> bloquearPorCodigo(@Param("codigo") UUID codigo);

    /** Con orden fijo, para que dos cierres masivos no se bloqueen entre sí. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sesion s where s.usuarioCodigo = :usuario and s.estado = :estado order by s.id")
    List<Sesion> bloquearPorUsuarioYEstado(@Param("usuario") UUID usuario, @Param("estado") EstadoSesion estado);

    List<Sesion> findByUsuarioCodigoAndEstado(UUID usuarioCodigo, EstadoSesion estado);

    default List<Sesion> findActivasByUsuarioCodigo(UUID usuarioCodigo) {
        return findByUsuarioCodigoAndEstado(usuarioCodigo, EstadoSesion.ACTIVA);
    }
}
