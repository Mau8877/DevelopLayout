package com.example.backend.features.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.features.auth.enums.EstadoSesion;
import com.example.backend.features.auth.model.Sesion;

/**
 * ! Con RLS forzado, solo se ven las sesiones del `app.current_user`, las de
 * ! `app.is_admin` o la fila cuyo hash coincide con `app.refresh_hash`.
 */
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByRefreshHash(String refreshHash);

    Optional<Sesion> findByRefreshHashAnterior(String refreshHashAnterior);

    Optional<Sesion> findByCodigo(UUID codigo);

    List<Sesion> findByUsuarioCodigoAndEstado(UUID usuarioCodigo, EstadoSesion estado);

    default List<Sesion> findActivasByUsuarioCodigo(UUID usuarioCodigo) {
        return findByUsuarioCodigoAndEstado(usuarioCodigo, EstadoSesion.ACTIVA);
    }
}
