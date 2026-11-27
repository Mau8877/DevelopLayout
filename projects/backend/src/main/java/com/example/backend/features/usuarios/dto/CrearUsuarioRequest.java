package com.example.backend.features.usuarios.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** ! Solo correo y roles: el cliente no elige estado, contraseña ni código. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CrearUsuarioRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 255, message = "El correo es demasiado largo")
        String email,
        @NotEmpty(message = "Elegí al menos un rol en el campo roles")
        @Size(max = 20, message = "El campo roles tiene demasiados elementos")
        List<@NotBlank(message = "El campo roles tiene un rol vacío") String> roles) {
}
