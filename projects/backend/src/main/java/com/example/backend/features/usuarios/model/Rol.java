package com.example.backend.features.usuarios.model;

import com.example.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Catálogo de roles. Tabla authentication.rol (RLS-OPT-OUT, solo lectura
 * para la app).
 */
@Entity
@Table(name = "rol", schema = "authentication")
@AttributeOverride(name = "codigo",
        column = @Column(name = "codigo_rol", updatable = false, nullable = false, unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rol extends BaseModel {

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
