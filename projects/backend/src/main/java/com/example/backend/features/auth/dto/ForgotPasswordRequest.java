package com.example.backend.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ForgotPasswordRequest(
        @NotBlank(message = "{auth.validacion.correoObligatorio}")
        @Email(message = "{auth.validacion.correoInvalido}")
        @Size(max = 255, message = "{auth.validacion.correoLargo}")
        String email) {
}
