package com.example.backend.features.auth.controller;

import java.util.List;
import java.util.UUID;

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
public class SesionAdminController {

    private final SesionAdminService sesiones;

    public SesionAdminController(SesionAdminService sesiones) {
        this.sesiones = sesiones;
    }

    @PostMapping("/sesiones/buscar")
    public ResponseEntity<ApiResponse<List<SesionResponse>>> buscar(@Valid @RequestBody BuscarSesionesRequest request) {
        Page<SesionResponse> pagina = sesiones.listarPorEmail(request.email(),
                Paginacion.de(request.page(), request.pageSize()));
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Sesiones obtenidas",
                PageMeta.de(pagina)));
    }

    @DeleteMapping("/sesiones/{codigo}")
    public ResponseEntity<ApiResponse<Void>> cerrar(@PathVariable UUID codigo) {
        sesiones.cerrar(codigo);
        return ResponseEntity.ok(ApiResponse.success(null, "Sesión cerrada"));
    }

    @PostMapping("/usuarios/{codigoUsuario}/sesiones/cerrar")
    public ResponseEntity<ApiResponse<Void>> cerrarTodas(@PathVariable UUID codigoUsuario) {
        int cerradas = sesiones.cerrarTodas(codigoUsuario);
        return ResponseEntity.ok(ApiResponse.success(null, "Sesiones cerradas: " + cerradas));
    }
}
