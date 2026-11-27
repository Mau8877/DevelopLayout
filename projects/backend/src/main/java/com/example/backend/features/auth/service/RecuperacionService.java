package com.example.backend.features.auth.service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.bitacora.BitacoraService;
import com.example.backend.common.bitacora.OrigenSolicitud;
import com.example.backend.common.bitacora.ResultadoBitacora;
import com.example.backend.common.exception.ValidationException;
import com.example.backend.common.util.Sha256;
import com.example.backend.config.RecuperacionEjecutorConfig;
import com.example.backend.features.auth.AuthAcciones;
import com.example.backend.features.auth.AuthErrorCodes;
import com.example.backend.features.auth.enums.MotivoRevocacion;
import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.security.RlsContext;

/**
 * Recuperación de contraseña: pedir el enlace y cambiarla.
 *
 * ! Ningún método público es @Transactional: ni BCrypt ni el SMTP corren con una
 * ! conexión tomada, y nunca se pide una segunda teniendo ya una.
 * ! La solicitud no mira la cuenta antes de responder: contenido y tiempo no
 * ! dependen de que exista.
 */
@Service
public class RecuperacionService {

    private static final Logger LOG = LoggerFactory.getLogger(RecuperacionService.class);

    private final SolicitudesRecuperacion solicitudes;
    private final PasswordResetTokenService tokens;
    private final InvitacionTokenService invitaciones;
    private final CorreoCuentaService correo;
    private final SesionService sesionService;
    private final LoginAttemptService loginAttempts;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RlsContext rlsContext;
    private final BitacoraService bitacora;
    private final Executor ejecutor;
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;

    public RecuperacionService(SolicitudesRecuperacion solicitudes, PasswordResetTokenService tokens,
            InvitacionTokenService invitaciones, CorreoCuentaService correo, SesionService sesionService, LoginAttemptService loginAttempts,
            UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, RlsContext rlsContext,
            BitacoraService bitacora,
            @Qualifier(RecuperacionEjecutorConfig.EJECUTOR) Executor ejecutor,
            PlatformTransactionManager transacciones) {
        this.solicitudes = solicitudes;
        this.tokens = tokens;
        this.invitaciones = invitaciones;
        this.correo = correo;
        this.sesionService = sesionService;
        this.loginAttempts = loginAttempts;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rlsContext = rlsContext;
        this.bitacora = bitacora;
        this.ejecutor = ejecutor;
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
        this.escritura = new TransactionTemplate(transacciones);
    }

    /** Pide el enlace. Retorna igual exista o no la cuenta; el resto ocurre después. */
    public void solicitar(String emailRecibido, OrigenSolicitud origen) {
        String email = emailRecibido.trim().toLowerCase(Locale.ROOT);
        String correoRef = Sha256.hex(email).substring(0, 12);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            if (!solicitudes.reservar(email)) {
                LOG.warn("Recuperación omitida: límite por correo alcanzado emailHash={}", correoRef);
                return;
            }
            try {
                ejecutor.execute(() -> procesar(email, correoRef, origen));
            } catch (TaskRejectedException e) {
                LOG.warn("Recuperación descartada: cola de envío llena emailHash={}", correoRef);
            }
        }
    }

    // * Corre en un hilo del ejecutor: nada puede escapar de acá.
    private void procesar(String email, String correoRef, OrigenSolicitud origen) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            Usuario usuario = lectura.execute(estado -> {
                rlsContext.usarCorreoDeLogin(email);
                return usuarioRepository.findByEmailIgnoreCase(email)
                        .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                        .orElse(null);
            });
            if (usuario == null) {
                return;
            }
            ResultadoBitacora resultado = ResultadoBitacora.EXITO;
            try {
                correo.enviarRecuperacion(usuario.getEmail(), tokens.emitir(usuario.getCodigo()));
                LOG.info("Enlace de recuperación enviado usuarioCodigo={}", usuario.getCodigo());
            } catch (RuntimeException e) {
                resultado = ResultadoBitacora.FALLO;
                // ! Sin el mensaje de la excepción: el de SMTP puede incluir el correo.
                LOG.error("No se pudo enviar el enlace usuarioCodigo={} causa={}", usuario.getCodigo(),
                        e.getClass().getSimpleName());
            }
            bitacora.registrarEnTransaccionPropia(usuario.getCodigo(), AuthAcciones.AUTH_RECUPERACION_SOLICITADA,
                    AuthAcciones.MODULO, null, null, resultado, Map.of("correoRef", correoRef), origen);
        } catch (RuntimeException e) {
            LOG.error("Falló una solicitud de recuperación emailHash={} causa={}", correoRef,
                    e.getClass().getSimpleName());
        }
    }

    /**
     * Cambia la contraseña con un token vigente, cierra todas las sesiones y
     * levanta el bloqueo de login.
     *
     * * El token se consume al FINAL, dentro de la transacción: si la contraseña
     * * elegida no sirve, el enlace sigue valiendo. Si la confirmación fallara
     * * después de consumirlo, la contraseña no cambia y hay que pedir otro.
     */
    public void restablecer(String token, String nuevaPassword) {
        UUID codigo = tokens.consultar(token).orElseThrow(RecuperacionService::enlaceInvalido);
        String hashActual = lectura.execute(estado -> {
            rlsContext.usarUsuario(codigo);
            return usuarioRepository.findByCodigo(codigo)
                    .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                    .map(Usuario::getPasswordHash)
                    .orElseThrow(RecuperacionService::enlaceInvalido);
        });
        if (passwordEncoder.matches(nuevaPassword, hashActual)) {
            throw new ValidationException(AuthErrorCodes.ERR_AUTH_06,
                    "La contraseña nueva tiene que ser distinta de la actual");
        }
        String hashNuevo = passwordEncoder.encode(nuevaPassword);

        String email = escritura.execute(estado -> {
            rlsContext.usarUsuario(codigo);
            // * Bloqueo exclusivo: un login en curso con la contraseña anterior o ya
            // * confirmó (y su sesión se revoca acá) o espera y ve el hash nuevo.
            Usuario usuario = usuarioRepository.bloquearParaCambio(codigo)
                    .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO && u.getPasswordHash().equals(hashActual))
                    .orElseThrow(RecuperacionService::enlaceInvalido);
            usuario.cambiarPassword(hashNuevo);
            // * Quien llegó hasta acá controla el correo: la invitación queda aceptada.
            if (usuario.isInvitacionPendiente()) {
                usuario.aceptarInvitacion();
                bitacora.registrar(codigo, AuthAcciones.AUTH_INVITACION_ACEPTADA, AuthAcciones.MODULO, "usuario",
                        codigo, ResultadoBitacora.EXITO, null);
            }
            sesionService.revocarTodas(codigo, MotivoRevocacion.RESET_PASSWORD);
            bitacora.registrar(codigo, AuthAcciones.AUTH_PASSWORD_RESTABLECIDA, AuthAcciones.MODULO, "usuario",
                    codigo, ResultadoBitacora.EXITO, null);
            usuarioRepository.flush();
            if (!tokens.consumir(token, codigo)) {
                throw enlaceInvalido();
            }
            return usuario.getEmail();
        });

        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            try {
                // * Si quedaba una invitación, ya no sirve: se borra para no dejarla suelta.
                invitaciones.anular(codigo);
                loginAttempts.reiniciar(email);
            } catch (RuntimeException e) {
                // * La contraseña ya cambió: el bloqueo vence solo.
                LOG.error("No se pudo levantar el bloqueo de login usuarioCodigo={} causa={}", codigo,
                        e.getClass().getSimpleName());
            }
            LOG.info("Contraseña restablecida usuarioCodigo={}", codigo);
        }
    }

    private static ValidationException enlaceInvalido() {
        return new ValidationException(AuthErrorCodes.ERR_AUTH_05,
                "El enlace no es válido o ya venció. Pedí uno nuevo");
    }
}
