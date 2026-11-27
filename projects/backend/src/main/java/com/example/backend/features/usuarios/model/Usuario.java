package com.example.backend.features.usuarios.model;

import com.example.backend.common.model.BaseModel;
import com.example.backend.features.usuarios.enums.EstadoUsuario;

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
 * Persona que puede iniciar sesión. Tabla authentication.usuario (RLS forzado).
 *
 * * Sin setters a propósito: cada cambio tiene su método con nombre. Hoy solo
 * * la contraseña; `estado` lo agregará quien lo necesite.
 * ! `passwordHash` nunca entra en un toString ni en un log: esta clase no
 * ! define toString.
 */
@Entity
@Table(name = "usuario", schema = "authentication")
@AttributeOverride(name = "codigo",
        column = @Column(name = "codigo_usuario", updatable = false, nullable = false, unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends BaseModel {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoUsuario estado;

    public Usuario(String email, String passwordHash, EstadoUsuario estado) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.estado = estado;
    }

    /** Recibe el hash BCrypt, nunca la contraseña. */
    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = nuevoHash;
    }
}
