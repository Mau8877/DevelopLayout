package com.example.backend.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El correo va en el body para que no quede en URLs ni en logs de acceso. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record BuscarSesionesRequest(
        @NotBlank(message = "{auth.validacion.correoObligatorio}")
        @Email(message = "{auth.validacion.correoInvalido}")
        @Size(max = 255, message = "{auth.validacion.correoLargo}")
        String email,
        Integer page,
        Integer pageSize) {
    // ! Datos personales: un record los incluiría en su toString.
    @Override
    public String toString() {
        return "BuscarSesionesRequest[oculto]";
    }
}
