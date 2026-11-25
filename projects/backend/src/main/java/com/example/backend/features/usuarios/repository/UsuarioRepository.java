package com.example.backend.features.usuarios.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.features.usuarios.model.Usuario;

/**
 * ! Con RLS forzado, estas consultas solo ven filas si el service fijó antes
 * ! `app.current_user` o `app.login_email` (ver BASE_DE_DATOS_PRIVADA_RLS.md).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCodigo(UUID codigo);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
