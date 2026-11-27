package com.example.backend.features.usuarios.repository;

import java.util.Collection;
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

    /** Todas las asignaciones del usuario, también las que se le quitaron. */
    @Query("select ur from UsuarioRol ur join fetch ur.rol where ur.usuario = :usuario")
    List<UsuarioRol> findTodasConRol(@Param("usuario") Usuario usuario);

    /** Las asignaciones activas de varios usuarios, con su rol, en una sola consulta. */
    @Query("select ur from UsuarioRol ur join fetch ur.rol r "
            + "where ur.usuario in :usuarios and ur.isActive = true and r.isActive = true")
    List<UsuarioRol> findActivasConRol(@Param("usuarios") Collection<Usuario> usuarios);
}
