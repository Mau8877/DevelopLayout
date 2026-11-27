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
 * * Sin setters a propósito: cada cambio tiene su método con nombre.
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

    // * La base no permite cambiarlo: sin `updatable = false`, Hibernate lo incluiría en cada UPDATE.
    @Column(name = "email", nullable = false, length = 255, updatable = false)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoUsuario estado;

    /** Mientras es true, la persona todavía no eligió su contraseña. */
    @Column(name = "invitacion_pendiente", nullable = false)
    private boolean invitacionPendiente;

    public Usuario(String email, String passwordHash, EstadoUsuario estado) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.estado = estado;
    }

    /**
     * Cuenta activa a la espera de que la persona elija su contraseña. El hash
     * es el de una contraseña aleatoria descartada: nadie puede usarla.
     */
    public static Usuario invitado(String email, String hashInutilizable) {
        Usuario usuario = new Usuario(email, hashInutilizable, EstadoUsuario.ACTIVO);
        usuario.invitacionPendiente = true;
        return usuario;
    }

    public void desactivar() {
        this.estado = EstadoUsuario.INACTIVO;
    }

    public void reactivar() {
        this.estado = EstadoUsuario.ACTIVO;
    }

    /** La persona ya demostró que controla el correo y eligió su contraseña. */
    public void aceptarInvitacion() {
        this.invitacionPendiente = false;
    }

    /** Recibe el hash BCrypt, nunca la contraseña. */
    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = nuevoHash;
    }
}
