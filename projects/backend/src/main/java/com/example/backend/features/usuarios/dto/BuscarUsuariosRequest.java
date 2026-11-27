package com.example.backend.features.usuarios.dto;

import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Size;

/** Criterios en el body para que el correo buscado no quede en URLs ni en logs de acceso. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record BuscarUsuariosRequest(
        Integer page,
        Integer pageSize,
        @Size(max = 255, message = "La búsqueda es demasiado larga")
        String search,
        EstadoUsuario estado,
        @Size(max = 50, message = "El rol no es válido")
        String rol) {
}
