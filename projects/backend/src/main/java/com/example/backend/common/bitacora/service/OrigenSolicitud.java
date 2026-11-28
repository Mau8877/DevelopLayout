package com.example.backend.common.bitacora.service;

/**
 * IP y user agent de la petición que originó un evento, para registrarlo desde un
 * hilo donde ya no hay petición en curso.
 */
public record OrigenSolicitud(String ip, String userAgent) {
    // ! Datos personales: un record los incluiría en su toString.
    @Override
    public String toString() {
        return "OrigenSolicitud[oculto]";
    }
}
