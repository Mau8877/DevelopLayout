package com.example.backend.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * `data` de POST /auth/login. Web: solo `usuario` (los tokens viajan en
 * cookies). Mobile: además `accessToken` y `refreshToken` en el body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(UsuarioSesionResponse usuario, String accessToken, String refreshToken) {
}
