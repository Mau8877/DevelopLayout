package com.example.backend.features.usuarios.dto;

/** `invitacionEnviada` en `false`: la cuenta existe pero el correo no salió. */
public record UsuarioCreadoResponse(UsuarioResponse usuario, boolean invitacionEnviada) {
}
