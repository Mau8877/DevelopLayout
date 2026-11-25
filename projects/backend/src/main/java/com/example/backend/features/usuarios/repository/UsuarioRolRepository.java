package com.example.backend.features.usuarios.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.features.usuarios.model.Rol;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.model.UsuarioRol;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    /**
     * Roles activos del usuario: cuentan solo si están activos tanto la
     * asignación (`UsuarioRol.isActive`) como el propio rol (`Rol.isActive`).
     */
    @Query("select ur.rol from UsuarioRol ur "
            + "where ur.usuario = :usuario and ur.isActive = true and ur.rol.isActive = true")
    List<Rol> findActivosByUsuario(@Param("usuario") Usuario usuario);
}
