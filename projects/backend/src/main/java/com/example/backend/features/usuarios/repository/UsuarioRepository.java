package com.example.backend.features.usuarios.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.features.usuarios.model.Usuario;

import jakarta.persistence.LockModeType;

/**
 * ! Con RLS forzado, estas consultas solo ven filas si el service fijó antes
 * ! `app.current_user` o `app.login_email` (ver BASE_DE_DATOS_PRIVADA_RLS.md).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCodigo(UUID codigo);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Bloqueo exclusivo de la fila hasta el fin de la transacción (cambio de contraseña). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.codigo = :codigo")
    Optional<Usuario> bloquearParaCambio(@Param("codigo") UUID codigo);

    /** Bloqueo compartido: varios logins conviven, pero esperan a un cambio de contraseña. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select u from Usuario u where u.codigo = :codigo")
    Optional<Usuario> bloquearParaLogin(@Param("codigo") UUID codigo);
}
