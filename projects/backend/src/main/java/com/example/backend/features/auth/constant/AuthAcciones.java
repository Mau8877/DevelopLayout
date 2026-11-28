package com.example.backend.features.auth.constant;

/** Acciones de auditoría (bitácora) del módulo de autenticación. */
public final class AuthAcciones {

    public static final String MODULO = "auth";
    public static final String AUTH_LOGIN_OK = "AUTH_LOGIN_OK";
    public static final String AUTH_LOGIN_FALLO = "AUTH_LOGIN_FALLO";
    public static final String AUTH_CUENTA_BLOQUEADA = "AUTH_CUENTA_BLOQUEADA";
    public static final String AUTH_REFRESH_REUSO = "AUTH_REFRESH_REUSO";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String AUTH_SESION_CERRADA_ADMIN = "AUTH_SESION_CERRADA_ADMIN";

    public static final String AUTH_RECUPERACION_SOLICITADA = "AUTH_RECUPERACION_SOLICITADA";
    public static final String AUTH_PASSWORD_RESTABLECIDA = "AUTH_PASSWORD_RESTABLECIDA";

    public static final String AUTH_SESION_CERRADA = "AUTH_SESION_CERRADA";

    public static final String AUTH_INVITACION_ACEPTADA = "AUTH_INVITACION_ACEPTADA";

    public static final String AUTH_PASSWORD_CAMBIADA = "AUTH_PASSWORD_CAMBIADA";
    public static final String AUTH_PASSWORD_CAMBIO_FALLIDO = "AUTH_PASSWORD_CAMBIO_FALLIDO";

    private AuthAcciones() {
    }
}
