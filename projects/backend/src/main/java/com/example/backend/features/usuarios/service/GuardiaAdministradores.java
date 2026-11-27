package com.example.backend.features.usuarios.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.features.auth.AuthErrorCodes;
import com.example.backend.features.auth.service.SesionService;
import com.example.backend.features.usuarios.RolNombres;
import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Rol;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.features.usuarios.repository.UsuarioRolRepository;
import com.example.backend.security.RlsContext;

/**
 * Único punto de entrada de los cambios de usuarios y roles: los pone en fila y
 * vuelve a comprobar a quien los pide.
 *
 * * Con eso siempre queda un administrador que puede entrar, sin contarlos:
 * * quitar ese poder lo hace un actor vigente (comprobado ya con el bloqueo) y
 * * nadie puede quitárselo a sí mismo. Quien tiene la invitación pendiente no
 * * tiene sesión, así que nunca es actor.
 * ! El bloqueo se toma ANTES de leer cualquier entidad: así lo que se lee
 * ! incluye lo que confirmó quien iba delante.
 */
@Service
@RequiredArgsConstructor
public class GuardiaAdministradores {

    private static final Logger LOG = LoggerFactory.getLogger(GuardiaAdministradores.class);
    private static final String CLAVE_BLOQUEO = "usuarios.administracion";

    private final JdbcTemplate jdbc;
    private final RlsContext rlsContext;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionService sesionService;

    /**
     * Deja la transacción con el contexto de administrador y el turno tomado
     * hasta que termine. Lanza 401 si quien pide el cambio ya no es un
     * administrador activo con su sesión abierta.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void exigirVigente(UUID actor, UUID sesionActual) {
        rlsContext.usarAdmin();
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext(?))", resultado -> null, CLAVE_BLOQUEO);

        boolean vigente = usuarioRepository.findByCodigo(actor)
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .filter(u -> usuarioRolRepository.findActivosByUsuario(u).stream()
                        .map(Rol::getNombre)
                        .anyMatch(RolNombres.SUPER_ADMIN::equals))
                .isPresent()
                && sesionService.estaActiva(sesionActual, actor);
        if (!vigente) {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "usuarios")) {
                LOG.warn("Cambio rechazado: quien lo pide ya no es un administrador vigente "
                        + "usuarioCodigo={} errorCode={}", actor, AuthErrorCodes.ERR_AUTH_04);
            }
            throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04,
                    "Tu sesión ya no es válida, iniciá sesión de nuevo");
        }
    }
}
