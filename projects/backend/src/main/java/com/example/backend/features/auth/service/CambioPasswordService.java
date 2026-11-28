package com.example.backend.features.auth.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.bitacora.BitacoraService;
import com.example.backend.common.bitacora.ResultadoBitacora;
import com.example.backend.common.exception.LockedException;
import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.common.exception.ValidationException;
import com.example.backend.features.auth.AuthAcciones;
import com.example.backend.features.auth.AuthErrorCodes;
import com.example.backend.features.auth.enums.EstadoSesion;
import com.example.backend.features.auth.enums.MotivoRevocacion;
import com.example.backend.features.auth.repository.SesionRepository;
import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.security.RlsContext;

/**
 * Cambio de la propia contraseña con la sesión iniciada. Cierra todas las
 * sesiones, también la que hace el cambio.
 *
 * ! Ningún método es @Transactional: ni BCrypt ni el SMTP corren con una
 * ! conexión tomada, y nunca se pide una segunda teniendo ya una.
 * ! La cuenta y la sesión salen del token. Nunca usa el contexto de administrador.
 */
@Service
public class CambioPasswordService {

    private static final Logger LOG = LoggerFactory.getLogger(CambioPasswordService.class);

    private final CambioPasswordIntentosService intentos;
    private final PasswordResetTokenService recuperaciones;
    private final InvitacionTokenService invitaciones;
    private final AvisoCambioPassword aviso;
    private final SesionService sesionService;
    private final SesionRepository sesionRepository;
    private final LoginAttemptService loginAttempts;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RlsContext rlsContext;
    private final BitacoraService bitacora;
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;

    public CambioPasswordService(CambioPasswordIntentosService intentos, PasswordResetTokenService recuperaciones,
            InvitacionTokenService invitaciones, AvisoCambioPassword aviso, SesionService sesionService,
            SesionRepository sesionRepository, LoginAttemptService loginAttempts, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, RlsContext rlsContext, BitacoraService bitacora,
            PlatformTransactionManager transacciones) {
        this.intentos = intentos;
        this.recuperaciones = recuperaciones;
        this.invitaciones = invitaciones;
        this.aviso = aviso;
        this.sesionService = sesionService;
        this.sesionRepository = sesionRepository;
        this.loginAttempts = loginAttempts;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rlsContext = rlsContext;
        this.bitacora = bitacora;
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
        this.escritura = new TransactionTemplate(transacciones);
    }

    /**
     * Cambia la contraseña de `usuario`, pedida desde `sesionActual`.
     *
     * * Los enlaces pendientes se anulan ANTES de confirmar: si eso falla, la
     * * contraseña no cambia.
     */
    public void cambiar(UUID usuario, UUID sesionActual, String passwordActual, String passwordNueva) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            String hashActual = leerHashConSesionVigente(usuario, sesionActual);
            comprobarPasswordActual(usuario, passwordActual, hashActual);
            if (passwordEncoder.matches(passwordNueva, hashActual)) {
                throw new ValidationException(AuthErrorCodes.ERR_AUTH_06, "auth.error.passwordRepetida");
            }
            String correoDelDueno = aplicar(usuario, sesionActual, hashActual, passwordEncoder.encode(passwordNueva));
            levantarBloqueoDeLogin(usuario, correoDelDueno);
            aviso.enviar(usuario, correoDelDueno);
            LOG.info("Contraseña cambiada usuarioCodigo={}", usuario);
        }
    }

    private String leerHashConSesionVigente(UUID usuario, UUID sesionActual) {
        return lectura.execute(estado -> {
            rlsContext.usarUsuario(usuario);
            if (!sesionService.estaActiva(sesionActual, usuario)) {
                throw sesionInvalida();
            }
            return usuarioRepository.findByCodigo(usuario)
                    .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                    .map(Usuario::getPasswordHash)
                    .orElseThrow(CambioPasswordService::sesionInvalida);
        });
    }

    // * El intento se reserva antes de comparar. Un fallo responde 400 y no 401:
    // * un 401 haría que el cliente diera la sesión por vencida.
    private void comprobarPasswordActual(UUID usuario, String passwordActual, String hashActual) {
        if (!intentos.reservar(usuario)) {
            LOG.warn("Cambio de contraseña bloqueado por intentos usuarioCodigo={} errorCode={}", usuario,
                    AuthErrorCodes.ERR_AUTH_13);
            throw new LockedException(AuthErrorCodes.ERR_AUTH_13, "auth.error.cambioBloqueado");
        }
        if (!passwordEncoder.matches(passwordActual, hashActual)) {
            bitacora.registrarEnTransaccionPropia(usuario, AuthAcciones.AUTH_PASSWORD_CAMBIO_FALLIDO,
                    AuthAcciones.MODULO, "usuario", usuario, ResultadoBitacora.FALLO, Map.of());
            LOG.warn("Cambio de contraseña rechazado usuarioCodigo={} errorCode={}", usuario,
                    AuthErrorCodes.ERR_AUTH_12);
            throw new ValidationException(AuthErrorCodes.ERR_AUTH_12, "auth.error.passwordActual");
        }
        intentos.reiniciar(usuario);
    }

    // * Mismo orden de bloqueos que la recuperación (usuario y después sesiones):
    // * un login con la contraseña anterior o ya confirmó, y su sesión se revoca
    // * acá, o espera y ve el hash nuevo.
    private String aplicar(UUID usuario, UUID sesionActual, String hashActual, String hashNuevo) {
        return escritura.execute(estado -> {
            rlsContext.usarUsuario(usuario);
            Usuario cuenta = usuarioRepository.bloquearParaCambio(usuario)
                    .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO && u.getPasswordHash().equals(hashActual))
                    .orElseThrow(CambioPasswordService::sesionInvalida);
            // ! Solo una sesión que sigue activa puede cambiar la contraseña.
            if (sesionRepository.bloquearPorUsuarioYEstado(usuario, EstadoSesion.ACTIVA).stream()
                    .noneMatch(s -> s.getCodigo().equals(sesionActual))) {
                throw sesionInvalida();
            }
            cuenta.cambiarPassword(hashNuevo);
            usuarioRepository.flush();
            recuperaciones.anular(usuario);
            invitaciones.anular(usuario);
            // * Las sesiones van al final: la lista de revocadas vive en Redis y no se
            // * deshace con la transacción.
            sesionService.revocarTodas(usuario, MotivoRevocacion.CAMBIO_PASSWORD);
            bitacora.registrar(usuario, AuthAcciones.AUTH_PASSWORD_CAMBIADA, AuthAcciones.MODULO, "usuario", usuario,
                    ResultadoBitacora.EXITO, null);
            return cuenta.getEmail();
        });
    }

    private void levantarBloqueoDeLogin(UUID usuario, String email) {
        try {
            loginAttempts.reiniciar(email);
        } catch (RuntimeException e) {
            // * La contraseña ya cambió: el bloqueo vence solo.
            LOG.error("No se pudo levantar el bloqueo de login usuarioCodigo={} causa={}", usuario,
                    e.getClass().getSimpleName());
        }
    }

    private static UnauthorizedException sesionInvalida() {
        return new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04, "auth.error.sesionInvalida");
    }
}
