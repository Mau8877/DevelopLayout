package com.example.backend.features.auth.controller;

import com.example.backend.common.idioma.Mensajes;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageMeta;
import com.example.backend.common.util.Paginacion;
import com.example.backend.features.auth.dto.BuscarSesionesRequest;
import com.example.backend.features.auth.dto.SesionResponse;
import com.example.backend.features.auth.service.SesionAdminService;

import jakarta.validation.Valid;

/** Administración de sesiones. La autorización la hace el service. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class SesionAdminController {

    private final SesionAdminService sesiones;
    private final Mensajes mensajes;

    @PostMapping("/sessions/search")
    public ResponseEntity<ApiResponse<List<SesionResponse>>> buscar(@Valid @RequestBody BuscarSesionesRequest request) {
        Page<SesionResponse> pagina = sesiones.listarPorEmail(request.email(),
                Paginacion.de(request.page(), request.pageSize()));
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), mensajes.de("auth.exito.sesionesObtenidas"),
                PageMeta.de(pagina)));
    }

    @DeleteMapping("/sessions/{codigo}")
    public ResponseEntity<ApiResponse<Void>> cerrar(@PathVariable UUID codigo) {
        sesiones.cerrar(codigo);
        return ResponseEntity.ok(ApiResponse.success(null, mensajes.de("auth.exito.sesionCerrada")));
    }

    @PostMapping("/users/{codigoUsuario}/sessions/revoke")
    public ResponseEntity<ApiResponse<Void>> cerrarTodas(@PathVariable UUID codigoUsuario) {
        int cerradas = sesiones.cerrarTodas(codigoUsuario);
        return ResponseEntity.ok(ApiResponse.success(null, mensajes.de("auth.exito.sesionesCerradas", cerradas)));
    }
}
