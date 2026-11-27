package com.example.backend.features.usuarios.model;

import com.example.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Asignación de un rol a un usuario. Tabla authentication.usuario_rol
 * (RLS forzado). Es una entidad propia y no un @ManyToMany porque la fila
 * de asociación tiene datos propios (codigo, isActive, timestamps).
 */
@Entity
@Table(name = "usuario_rol", schema = "authentication")
@AttributeOverride(name = "codigo",
        column = @Column(name = "codigo_usuario_rol", updatable = false, nullable = false, unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsuarioRol extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false, updatable = false)
    private Rol rol;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public UsuarioRol(Usuario usuario, Rol rol) {
        this.usuario = usuario;
        this.rol = rol;
        this.isActive = true;
    }

    /** Vuelve a dar un rol que se había quitado: se reutiliza la misma fila. */
    public void activar() {
        this.isActive = true;
    }

    /** Quitar un rol no borra la asignación. */
    public void desactivar() {
        this.isActive = false;
    }
}
