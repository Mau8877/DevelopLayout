package com.example.backend.features.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.bitacora.service.BitacoraService;
import com.example.backend.common.bitacora.enums.ResultadoBitacora;
import com.example.backend.common.exception.NotFoundException;
import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.common.exception.ValidationException;
import com.example.backend.features.auth.constant.AuthAcciones;
import com.example.backend.features.auth.constant.AuthErrorCodes;
import com.example.backend.features.auth.dto.MiSesionResponse;
import com.example.backend.features.auth.enums.EstadoSesion;
import com.example.backend.features.auth.enums.MotivoRevocacion;
import com.example.backend.features.auth.model.Sesion;
import com.example.backend.features.auth.repository.SesionRepository;
import com.example.backend.security.RlsContext;

/**
 * Cada usuario ve y cierra SUS sesiones.
 *
 * ! Nunca usa el contexto de administrador de RLS, tampoco si quien llama es
 * ! administrador. El usuario sale siempre del token.
 * ! Solo una sesión que sigue activa puede cerrar otras: ver
 * ! `bloquearExigiendoLaActual`.
 */
@Service
@RequiredArgsConstructor
public class MisSesionesService {

    private static final Logger LOG = LoggerFactory.getLogger(MisSesionesService.class);

    private final SesionRepository sesionRepository;
    private final SesionService sesionService;
    private final RlsContext rlsContext;
    private final BitacoraService bitacora;

    /** Sesiones vigentes del usuario, con la actual primero. */
    @Transactional(readOnly = true)
    public Page<MiSesionResponse> listar(UUID usuario, UUID sesionActual, Pageable pageable) {
        rlsContext.usarUsuario(usuario);
        return sesionRepository
                .listarVigentesConActualPrimero(usuario, EstadoSesion.ACTIVA, Instant.now(), sesionActual, pageable)
                .map(s -> new MiSesionResponse(s.getCodigo(), s.getTipoCliente(), s.getIp(), s.getUserAgent(),
                        s.getCreatedAt(), s.getUltimoUsoEn(), s.getExpiraEn(), s.getCodigo().equals(sesionActual)));
    }

    /** Cierra una sesión propia que no sea la actual. Si ya estaba cerrada no hace nada. */
    @Transactional
    public void cerrar(UUID usuario, UUID sesionActual, UUID codigo) {
        if (codigo.equals(sesionActual)) {
            throw new ValidationException(AuthErrorCodes.ERR_AUTH_10,
                    "auth.error.sesionActual");
        }
        List<Sesion> activas = bloquearExigiendoLaActual(usuario, sesionActual);
        activas.stream().filter(s -> s.getCodigo().equals(codigo)).findFirst().ifPresentOrElse(
                this::revocarYRegistrar,
                // * Propia y ya cerrada: nada que hacer. Ajena o inexistente: el mismo 404.
                () -> sesionRepository.findByCodigo(codigo)
                        .filter(s -> s.getUsuarioCodigo().equals(usuario))
                        .orElseThrow(() -> new NotFoundException(AuthErrorCodes.ERR_AUTH_09, "auth.error.sesionNoExiste")));
    }

    /**
     * Cierra todas las `ACTIVA` menos la actual, también las vencidas: su último
     * access token podría seguir vigente.
     *
     * @return cuántas se cerraron.
     */
    @Transactional
    public int cerrarOtras(UUID usuario, UUID sesionActual) {
        List<Sesion> otras = bloquearExigiendoLaActual(usuario, sesionActual).stream()
                .filter(s -> !s.getCodigo().equals(sesionActual))
                .toList();
        otras.forEach(this::revocarYRegistrar);
        return otras.size();
    }

    // * Se bloquean TODAS las activas (con orden fijo) y la actual debe seguir
    // * entre ellas. Bloquear solo la que se cierra no alcanza: dos sesiones que se
    // * cierran una a otra pasan ambas la autenticación y quedarían las dos
    // * cerradas. Al esperar el bloqueo, PostgreSQL deja fuera las filas que la
    // * otra transacción acaba de revocar.
    private List<Sesion> bloquearExigiendoLaActual(UUID usuario, UUID sesionActual) {
        rlsContext.usarUsuario(usuario);
        List<Sesion> activas = sesionRepository.bloquearPorUsuarioYEstado(usuario, EstadoSesion.ACTIVA);
        if (activas.stream().noneMatch(s -> s.getCodigo().equals(sesionActual))) {
            throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04,
                    "auth.error.sesionInvalida");
        }
        return activas;
    }

    private void revocarYRegistrar(Sesion sesion) {
        sesionService.revocar(sesion, MotivoRevocacion.USUARIO);
        bitacora.registrar(AuthAcciones.AUTH_SESION_CERRADA, AuthAcciones.MODULO, "sesion", sesion.getCodigo(),
                ResultadoBitacora.EXITO, Map.of("motivo", MotivoRevocacion.USUARIO.name()));
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            LOG.info("Sesión cerrada por su dueño sesionCodigo={} usuarioCodigo={}", sesion.getCodigo(),
                    sesion.getUsuarioCodigo());
        }
    }
}
