package com.example.backend.features.usuarios.enums;

/**
 * Estado de negocio de un usuario.
 *
 * * Se persiste con EnumType.STRING. No es el bloqueo temporal por intentos
 * * fallidos de login (ese vive en Redis, ver SEGURIDAD_AUTH_BACKEND.md).
 */
public enum EstadoUsuario {
    ACTIVO,
    INACTIVO
}
