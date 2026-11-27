package com.example.backend.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El token va en el body, nunca en la URL de la API. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record ResetPasswordRequest(
        @NotBlank(message = "El enlace no es válido")
        @Size(max = 100, message = "El enlace no es válido")
        String token,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @MaximoBytes(72)
        String password) {
}
