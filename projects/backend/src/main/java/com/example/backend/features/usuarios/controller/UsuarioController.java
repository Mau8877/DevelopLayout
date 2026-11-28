package com.example.backend.features.usuarios.controller;

import com.example.backend.common.idioma.Mensajes;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageMeta;
import com.example.backend.common.util.Paginacion;
import com.example.backend.features.usuarios.dto.BuscarUsuariosRequest;
import com.example.backend.features.usuarios.dto.CambiarRolesRequest;
import com.example.backend.features.usuarios.dto.CrearUsuarioRequest;
import com.example.backend.features.usuarios.dto.ReenvioInvitacionResponse;
import com.example.backend.features.usuarios.dto.UsuarioCreadoResponse;
import com.example.backend.features.usuarios.dto.UsuarioResponse;
import com.example.backend.features.usuarios.repository.UsuarioBusquedaRepository;
import com.example.backend.features.usuarios.service.UsuarioAdminService;
import com.example.backend.security.annotation.CurrentSessionId;
import com.example.backend.security.annotation.CurrentUserId;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

/** Administración de usuarios. La autorización la hace el service. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioAdminService usuarios;
    private final Mensajes mensajes;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> buscarUsuarios(
            @Valid @RequestBody BuscarUsuariosRequest request) {
        Page<UsuarioResponse> pagina = usuarios.buscar(new UsuarioBusquedaRepository.Filtro(
                texto(request.search()), request.estado(), texto(request.rol())),
                Paginacion.de(request.page(), request.pageSize()));
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), mensajes.de("usuarios.exito.obtenidos"),
                PageMeta.de(pagina)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioCreadoResponse>> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequest request,
            @Parameter(hidden = true) @CurrentUserId UUID actor,
            @Parameter(hidden = true) @CurrentSessionId UUID sesion) {
        UsuarioCreadoResponse creado = usuarios.crear(new UsuarioAdminService.Actor(actor, sesion),
                request.email(), request.roles());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(creado,
                creado.invitacionEnviada() ? mensajes.de("usuarios.exito.creado")
                        : mensajes.de("usuarios.exito.creadoSinCorreo")));
    }

    @PostMapping("/{codigo}/resend-invitation")
    public ResponseEntity<ApiResponse<ReenvioInvitacionResponse>> reenviarInvitacion(@PathVariable UUID codigo,
            @Parameter(hidden = true) @CurrentUserId UUID actor,
            @Parameter(hidden = true) @CurrentSessionId UUID sesion) {
        ReenvioInvitacionResponse reenvio = usuarios.reenviarInvitacion(
                new UsuarioAdminService.Actor(actor, sesion), codigo);
        return ResponseEntity.ok(ApiResponse.success(reenvio, reenvio.invitacionEnviada()
                ? mensajes.de("usuarios.exito.reenviada") : mensajes.de("usuarios.exito.reenvioFallido")));
    }

    @PostMapping("/{codigo}/deactivate")
    public ResponseEntity<ApiResponse<UsuarioResponse>> desactivarUsuario(@PathVariable UUID codigo,
            @Parameter(hidden = true) @CurrentUserId UUID actor,
            @Parameter(hidden = true) @CurrentSessionId UUID sesion) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarios.desactivar(new UsuarioAdminService.Actor(actor, sesion), codigo), mensajes.de("usuarios.exito.desactivado")));
    }

    @PostMapping("/{codigo}/reactivate")
    public ResponseEntity<ApiResponse<UsuarioResponse>> reactivarUsuario(@PathVariable UUID codigo,
            @Parameter(hidden = true) @CurrentUserId UUID actor,
            @Parameter(hidden = true) @CurrentSessionId UUID sesion) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarios.reactivar(new UsuarioAdminService.Actor(actor, sesion), codigo), mensajes.de("usuarios.exito.reactivado")));
    }

    @PutMapping("/{codigo}/roles")
    public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarRolesUsuario(@PathVariable UUID codigo,
            @Valid @RequestBody CambiarRolesRequest request,
            @Parameter(hidden = true) @CurrentUserId UUID actor,
            @Parameter(hidden = true) @CurrentSessionId UUID sesion) {
        return ResponseEntity.ok(ApiResponse.success(usuarios.cambiarRoles(
                new UsuarioAdminService.Actor(actor, sesion), codigo, request.roles()), mensajes.de("usuarios.exito.rolesActualizados")));
    }

    // * Vacío o solo espacios es «sin filtrar».
    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
