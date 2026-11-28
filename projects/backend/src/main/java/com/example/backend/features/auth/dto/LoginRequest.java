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
        @NotBlank(message = "{auth.validacion.correoObligatorio}")
        @Email(message = "{auth.validacion.correoInvalido}")
        @Size(max = 255, message = "{auth.validacion.correoLargo}")
        String email,
        @NotBlank(message = "{auth.validacion.passwordObligatoria}")
        @Size(min = 8, max = 72, message = "{auth.validacion.passwordLargo}")
        String password) {
}
