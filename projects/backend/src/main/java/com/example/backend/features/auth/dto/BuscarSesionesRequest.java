package com.example.backend.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El correo va en el body para que no quede en URLs ni en logs de acceso. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record BuscarSesionesRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 255, message = "El correo es demasiado largo")
        String email,
        Integer page,
        Integer pageSize) {
}
