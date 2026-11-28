package com.example.backend.features.usuarios.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.backend.features.usuarios.enums.EstadoUsuario;

/** ! Nunca lleva el hash, el id interno ni enlaces. */
public record UsuarioResponse(
        UUID codigo,
        String email,
        List<String> roles,
        EstadoUsuario estado,
        boolean invitacionPendiente,
        Instant creadoEn) {
    // ! Datos personales: un record los incluiría en su toString.
    @Override
    public String toString() {
        return "UsuarioResponse[oculto]";
    }
}
