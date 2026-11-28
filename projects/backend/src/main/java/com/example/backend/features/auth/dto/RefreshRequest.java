package com.example.backend.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Body de /refresh y /logout para mobile (web usa la cookie y no manda body).
 * Una propiedad desconocida se rechaza (400).
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record RefreshRequest(String refreshToken) {
    // ! Sin el token: un record lo incluiría en su toString.
    @Override
    public String toString() {
        return "RefreshRequest[oculto]";
    }
}
