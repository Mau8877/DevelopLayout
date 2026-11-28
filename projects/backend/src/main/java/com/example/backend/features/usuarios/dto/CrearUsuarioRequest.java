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
        @NotBlank(message = "{auth.validacion.correoObligatorio}")
        @Email(message = "{auth.validacion.correoInvalido}")
        @Size(max = 255, message = "{auth.validacion.correoLargo}")
        String email,
        @NotEmpty(message = "{usuarios.validacion.rolRequerido}")
        @Size(max = 20, message = "{usuarios.validacion.rolesDemasiados}")
        List<@NotBlank(message = "{usuarios.validacion.rolVacio}") String> roles) {
    // ! Datos personales: un record los incluiría en su toString.
    @Override
    public String toString() {
        return "CrearUsuarioRequest[oculto]";
    }
}
