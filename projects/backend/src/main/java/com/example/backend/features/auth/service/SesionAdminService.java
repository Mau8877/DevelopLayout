package com.example.backend.features.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.bitacora.BitacoraService;
import com.example.backend.common.bitacora.ResultadoBitacora;
import com.example.backend.common.exception.NotFoundException;
import com.example.backend.features.auth.AuthAcciones;
import com.example.backend.features.auth.AuthErrorCodes;
import com.example.backend.features.auth.dto.SesionResponse;
import com.example.backend.features.auth.enums.EstadoSesion;
import com.example.backend.features.auth.enums.MotivoRevocacion;
import com.example.backend.features.auth.model.Sesion;
import com.example.backend.features.auth.repository.SesionRepository;
import com.example.backend.features.usuarios.service.UsuarioConsultaService;
import com.example.backend.security.RlsContext;

/**
 * El administrador lista y cierra sesiones de cualquier usuario.
 *
 * ! Cada método público exige SUPER_ADMIN y solo después abre el contexto de
 * ! administrador de RLS.
 */
@Service
@RequiredArgsConstructor
public class SesionAdminService {

    private static final Logger LOG = LoggerFactory.getLogger(SesionAdminService.class);
    private static final String ES_ADMIN = "hasRole('SUPER_ADMIN')";

    private final SesionRepository sesionRepository;
    private final SesionService sesionService;
    private final UsuarioConsultaService usuarios;
    private final RlsContext rlsContext;
    private final BitacoraService bitacora;

    /** Sesiones vigentes del usuario con ese correo, la más reciente primero. */
    @PreAuthorize(ES_ADMIN)
    @Transactional(readOnly = true)
    public Page<SesionResponse> listarPorEmail(String email, Pageable pageable) {
        rlsContext.usarAdmin();
        Optional<UUID> usuario = usuarios.codigoPorEmail(email);
        // * Un correo inexistente da la misma respuesta que un usuario sin sesiones.
        if (usuario.isEmpty()) {
            return Page.empty(pageable);
        }
        Pageable ordenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return sesionRepository.findByUsuarioCodigoAndEstadoAndExpiraEnAfter(usuario.get(), EstadoSesion.ACTIVA,
                Instant.now(), ordenado).map(SesionAdminService::aRespuesta);
    }

    /** Cierra una sesión. Si ya estaba cerrada no hace nada. */
    @PreAuthorize(ES_ADMIN)
    @Transactional
    public void cerrar(UUID codigoSesion) {
        rlsContext.usarAdmin();
        Sesion sesion = sesionRepository.bloquearPorCodigo(codigoSesion)
                .orElseThrow(() -> new NotFoundException(AuthErrorCodes.ERR_AUTH_09, "La sesión no existe"));
        if (sesion.getEstado() == EstadoSesion.ACTIVA) {
            revocarYRegistrar(sesion);
        }
    }

    /**
     * Cierra todas las sesiones `ACTIVA` del usuario, también las vencidas: su
     * último access token podría seguir vigente.
     *
     * @return cuántas se cerraron.
     */
    @PreAuthorize(ES_ADMIN)
    @Transactional
    public int cerrarTodas(UUID codigoUsuario) {
        rlsContext.usarAdmin();
        List<Sesion> activas = sesionRepository.bloquearPorUsuarioYEstado(codigoUsuario, EstadoSesion.ACTIVA);
        activas.forEach(this::revocarYRegistrar);
        return activas.size();
    }

    private void revocarYRegistrar(Sesion sesion) {
        sesionService.revocar(sesion, MotivoRevocacion.ADMIN);
        bitacora.registrar(AuthAcciones.AUTH_SESION_CERRADA_ADMIN, AuthAcciones.MODULO, "sesion", sesion.getCodigo(),
                ResultadoBitacora.EXITO, Map.of("usuarioAfectado", sesion.getUsuarioCodigo()));
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            LOG.info("Sesión cerrada por un administrador sesionCodigo={} usuarioAfectado={}", sesion.getCodigo(),
                    sesion.getUsuarioCodigo());
        }
    }

    private static SesionResponse aRespuesta(Sesion s) {
        return new SesionResponse(s.getCodigo(), s.getUsuarioCodigo(), s.getTipoCliente(), s.getIp(),
                s.getUserAgent(), s.getCreatedAt(), s.getUltimoUsoEn(), s.getExpiraEn());
    }
}
