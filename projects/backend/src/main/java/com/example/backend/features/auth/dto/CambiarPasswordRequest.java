package com.example.backend.features.auth.dto;

import com.example.backend.common.validation.PasswordNoComun;
import com.example.backend.features.auth.validation.MaximoBytes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ! Solo las dos contraseñas: la cuenta sale del token, nunca del body. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CambiarPasswordRequest(
        @NotBlank(message = "{auth.validacion.passwordActualObligatoria}")
        @Size(max = 72, message = "{auth.validacion.passwordMaximo}")
        @MaximoBytes(72)
        String passwordActual,
        @NotBlank(message = "{auth.validacion.passwordObligatoria}")
        @Size(min = 8, max = 72, message = "{auth.validacion.passwordLargo}")
        @MaximoBytes(72)
        @PasswordNoComun
        String passwordNueva) {

    // ! Sin las contraseñas: un record las incluiría en su toString.
    @Override
    public String toString() {
        return "CambiarPasswordRequest[oculto]";
    }
}
