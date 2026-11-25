package com.example.backend.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body de login. Solo `email` y `password`: una propiedad desconocida se
 * rechaza (400) en vez de ignorarse (sin mass assignment).
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 255, message = "El correo es demasiado largo")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password) {
}
