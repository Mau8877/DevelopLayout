package com.example.backend.features.usuarios.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** El conjunto completo de roles que debe quedar. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CambiarRolesRequest(
        @NotEmpty(message = "Elegí al menos un rol en el campo roles")
        @Size(max = 20, message = "El campo roles tiene demasiados elementos")
        List<@NotBlank(message = "El campo roles tiene un rol vacío") String> roles) {
}
