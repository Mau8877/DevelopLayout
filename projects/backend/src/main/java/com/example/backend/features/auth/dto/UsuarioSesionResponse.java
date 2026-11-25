package com.example.backend.features.auth.dto;

import java.util.List;
import java.util.UUID;

/** Usuario de la sesión actual. Sin `id` interno ni `passwordHash`. */
public record UsuarioSesionResponse(UUID codigo, String email, List<String> roles) {
}
