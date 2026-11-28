package com.example.backend.common.bitacora.service;

/**
 * IP y user agent de la petición que originó un evento, para registrarlo desde un
 * hilo donde ya no hay petición en curso.
 */
public record OrigenSolicitud(String ip, String userAgent) {
}
