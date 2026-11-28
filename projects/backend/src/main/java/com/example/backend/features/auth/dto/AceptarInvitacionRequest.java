package com.example.backend.features.auth.dto;

import com.example.backend.features.auth.validation.MaximoBytes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El token va en el body, nunca en la URL de la API. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AceptarInvitacionRequest(
        @NotBlank(message = "{auth.validacion.enlace}")
        @Size(max = 100, message = "{auth.validacion.enlace}")
        String token,
        @NotBlank(message = "{auth.validacion.passwordObligatoria}")
        @Size(min = 8, max = 72, message = "{auth.validacion.passwordLargo}")
        @MaximoBytes(72)
        String password) {
}
