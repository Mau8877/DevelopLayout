package com.example.backend.features.usuarios.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageMeta;
import com.example.backend.common.util.Paginacion;
import com.example.backend.features.usuarios.dto.BuscarUsuariosRequest;
import com.example.backend.features.usuarios.dto.CrearUsuarioRequest;
import com.example.backend.features.usuarios.dto.ReenvioInvitacionResponse;
import com.example.backend.features.usuarios.dto.UsuarioCreadoResponse;
import com.example.backend.features.usuarios.dto.UsuarioResponse;
import com.example.backend.features.usuarios.repository.UsuarioBusquedaRepository;
import com.example.backend.features.usuarios.service.UsuarioAdminService;
import com.example.backend.security.CurrentSessionId;
import com.example.backend.security.CurrentUserId;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

/** Administración de usuarios. La autorización la hace el service. */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioAdminService usuarios;

    @PostMapping("/buscar")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> buscarUsuarios(
            @Valid @RequestBody BuscarUsuariosRequest request) {
        Page<UsuarioResponse> pagina = usuarios.buscar(new UsuarioBusquedaRepository.Filtro(
                texto(request.search()), request.estado(), texto(request.rol())),
                Paginacion.de(request.page(), request.pageSize()));
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Usuarios obtenidos",
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
                creado.invitacionEnviada() ? "Usuario creado e invitación enviada"
                        : "Usuario creado, pero no se pudo enviar la invitación. Podés reenviarla"));
    }

    @PostMapping("/{codigo}/reenviar-invitacion")
    public ResponseEntity<ApiResponse<ReenvioInvitacionResponse>> reenviarInvitacion(@PathVariable UUID codigo,
            @Parameter(hidden = true) @CurrentUserId UUID actor,
            @Parameter(hidden = true) @CurrentSessionId UUID sesion) {
        ReenvioInvitacionResponse reenvio = usuarios.reenviarInvitacion(
                new UsuarioAdminService.Actor(actor, sesion), codigo);
        return ResponseEntity.ok(ApiResponse.success(reenvio, reenvio.invitacionEnviada()
                ? "Invitación reenviada" : "No se pudo enviar la invitación. Probá de nuevo"));
    }

    @PostMapping("/{codigo}/desactivar")
    public ResponseEntity<ApiResponse<UsuarioResponse>> desactivarUsuario(@PathVariable UUID codigo,
            @Parameter(hidden = true) @CurrentUserId UUID actor,
            @Parameter(hidden = true) @CurrentSessionId UUID sesion) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarios.desactivar(new UsuarioAdminService.Actor(actor, sesion), codigo), "Usuario desactivado"));
    }

    @PostMapping("/{codigo}/reactivar")
    public ResponseEntity<ApiResponse<UsuarioResponse>> reactivarUsuario(@PathVariable UUID codigo,
            @Parameter(hidden = true) @CurrentUserId UUID actor,
            @Parameter(hidden = true) @CurrentSessionId UUID sesion) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarios.reactivar(new UsuarioAdminService.Actor(actor, sesion), codigo), "Usuario reactivado"));
    }

    // * Vacío o solo espacios es «sin filtrar».
    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
