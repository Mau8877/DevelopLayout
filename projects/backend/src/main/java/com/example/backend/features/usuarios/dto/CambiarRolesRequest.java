package com.example.backend.features.usuarios.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** El conjunto completo de roles que debe quedar. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CambiarRolesRequest(
        @NotEmpty(message = "{usuarios.validacion.rolRequerido}")
        @Size(max = 20, message = "{usuarios.validacion.rolesDemasiados}")
        List<@NotBlank(message = "{usuarios.validacion.rolVacio}") String> roles) {
}
