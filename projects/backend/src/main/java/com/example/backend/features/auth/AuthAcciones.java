package com.example.backend.features.auth;

/** Acciones de auditoría (bitácora) del módulo de autenticación. */
public final class AuthAcciones {

    public static final String MODULO = "auth";
    public static final String AUTH_LOGIN_OK = "AUTH_LOGIN_OK";
    public static final String AUTH_LOGIN_FALLO = "AUTH_LOGIN_FALLO";
    public static final String AUTH_CUENTA_BLOQUEADA = "AUTH_CUENTA_BLOQUEADA";
    public static final String AUTH_REFRESH_REUSO = "AUTH_REFRESH_REUSO";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String AUTH_SESION_CERRADA_ADMIN = "AUTH_SESION_CERRADA_ADMIN";

    private AuthAcciones() {
    }
}
