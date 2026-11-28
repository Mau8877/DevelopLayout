package com.example.backend.features.auth.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.bitacora.service.BitacoraService;
import com.example.backend.common.bitacora.enums.ResultadoBitacora;
import com.example.backend.common.exception.ValidationException;
import com.example.backend.features.auth.constant.AuthAcciones;
import com.example.backend.features.auth.constant.AuthErrorCodes;
import com.example.backend.features.auth.enums.MotivoRevocacion;
import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.security.RlsContext;

/**
 * La persona invitada elige su contraseña. Sigue el mismo patrón que el
 * restablecimiento.
 *
 * ! No es @Transactional: BCrypt no corre con una conexión tomada.
 * ! La autoridad es `invitacionPendiente` en la base: un token que sobreviva a
 * ! la aceptación no sirve.
 */
@Service
public class InvitacionService {

    private static final Logger LOG = LoggerFactory.getLogger(InvitacionService.class);

    private final InvitacionTokenService invitaciones;
    private final PasswordResetTokenService recuperaciones;
    private final SesionService sesionService;
    private final LoginAttemptService loginAttempts;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RlsContext rlsContext;
    private final BitacoraService bitacora;
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;

    public InvitacionService(InvitacionTokenService invitaciones, PasswordResetTokenService recuperaciones,
            SesionService sesionService, LoginAttemptService loginAttempts, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, RlsContext rlsContext, BitacoraService bitacora,
            PlatformTransactionManager transacciones) {
        this.invitaciones = invitaciones;
        this.recuperaciones = recuperaciones;
        this.sesionService = sesionService;
        this.loginAttempts = loginAttempts;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rlsContext = rlsContext;
        this.bitacora = bitacora;
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
        this.escritura = new TransactionTemplate(transacciones);
    }

    /** Guarda la contraseña y deja la invitación aceptada. No inicia sesión. */
    public void aceptar(String token, String password) {
        UUID codigo = invitaciones.consultar(token).orElseThrow(InvitacionService::enlaceInvalido);
        lectura.executeWithoutResult(estado -> {
            rlsContext.usarUsuario(codigo);
            if (usuarioRepository.findByCodigo(codigo).filter(InvitacionService::puedeAceptar).isEmpty()) {
                throw enlaceInvalido();
            }
        });
        String hash = passwordEncoder.encode(password);

        String email = escritura.execute(estado -> {
            rlsContext.usarUsuario(codigo);
            // * Con la fila bloqueada se vuelve a comprobar: pudo desactivarse o aceptarse.
            Usuario usuario = usuarioRepository.bloquearParaCambio(codigo)
                    .filter(InvitacionService::puedeAceptar)
                    .orElseThrow(InvitacionService::enlaceInvalido);
            usuario.cambiarPassword(hash);
            usuario.aceptarInvitacion();
            sesionService.revocarTodas(codigo, MotivoRevocacion.RESET_PASSWORD);
            bitacora.registrar(codigo, AuthAcciones.AUTH_INVITACION_ACEPTADA, AuthAcciones.MODULO, "usuario",
                    codigo, ResultadoBitacora.EXITO, null);
            usuarioRepository.flush();
            // * Al final: una contraseña rechazada no gasta el enlace.
            if (!invitaciones.consumir(token, codigo)) {
                throw enlaceInvalido();
            }
            return usuario.getEmail();
        });

        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            try {
                recuperaciones.anular(codigo);
                loginAttempts.reiniciar(email);
            } catch (RuntimeException e) {
                // * La cuenta ya quedó activada: ambos vencen solos.
                LOG.error("No se pudo limpiar tras aceptar la invitación usuarioCodigo={} causa={}", codigo,
                        e.getClass().getSimpleName());
            }
            LOG.info("Invitación aceptada usuarioCodigo={}", codigo);
        }
    }

    private static boolean puedeAceptar(Usuario usuario) {
        return usuario.getEstado() == EstadoUsuario.ACTIVO && usuario.isInvitacionPendiente();
    }

    // * El mismo mensaje para todos los motivos: no se dice por qué no sirve.
    private static ValidationException enlaceInvalido() {
        return new ValidationException(AuthErrorCodes.ERR_AUTH_11,
                "auth.error.invitacionInvalida");
    }
}
