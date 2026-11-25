package com.example.backend.features.usuarios;

/**
 * Nombres de rol conocidos por el sistema.
 *
 * * Los roles son datos (filas de authentication.rol), no un enum cerrado:
 * * estas constantes solo evitan strings sueltos en el código.
 */
public final class RolNombres {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String USUARIO = "USUARIO";

    private RolNombres() {
    }
}
