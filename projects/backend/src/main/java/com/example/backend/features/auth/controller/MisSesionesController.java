package com.example.backend.features.auth.controller;

import com.example.backend.common.idioma.Mensajes;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageMeta;
import com.example.backend.common.util.Paginacion;
import com.example.backend.features.auth.dto.MiSesionResponse;
import com.example.backend.features.auth.service.MisSesionesService;
import com.example.backend.security.annotation.CurrentSessionId;
import com.example.backend.security.annotation.CurrentUserId;

import io.swagger.v3.oas.annotations.Parameter;

/** Sesiones del usuario autenticado: el usuario y la sesión actual salen del token. */
@RestController
@RequestMapping("/api/v1/auth/sessions")
@RequiredArgsConstructor
public class MisSesionesController {

    private final MisSesionesService sesiones;
    private final Mensajes mensajes;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MiSesionResponse>>> listarMisSesiones(
            @Parameter(hidden = true) @CurrentUserId UUID usuario,
            @Parameter(hidden = true) @CurrentSessionId UUID sesionActual,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        Page<MiSesionResponse> pagina = sesiones.listar(usuario, sesionActual, Paginacion.de(page, pageSize));
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), mensajes.de("auth.exito.sesionesObtenidas"),
                PageMeta.de(pagina)));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<ApiResponse<Void>> cerrarMiSesion(
            @Parameter(hidden = true) @CurrentUserId UUID usuario,
            @Parameter(hidden = true) @CurrentSessionId UUID sesionActual,
            @PathVariable UUID codigo) {
        sesiones.cerrar(usuario, sesionActual, codigo);
        return ResponseEntity.ok(ApiResponse.success(null, mensajes.de("auth.exito.sesionCerrada")));
    }

    @PostMapping("/revoke-others")
    public ResponseEntity<ApiResponse<Void>> cerrarMisOtrasSesiones(
            @Parameter(hidden = true) @CurrentUserId UUID usuario,
            @Parameter(hidden = true) @CurrentSessionId UUID sesionActual) {
        int cerradas = sesiones.cerrarOtras(usuario, sesionActual);
        return ResponseEntity.ok(ApiResponse.success(null, mensajes.de("auth.exito.sesionesCerradas", cerradas)));
    }
}
