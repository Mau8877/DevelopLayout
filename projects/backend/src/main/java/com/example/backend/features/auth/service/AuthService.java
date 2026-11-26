package com.example.backend.features.auth.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.example.backend.common.bitacora.BitacoraService;
import com.example.backend.common.bitacora.ResultadoBitacora;
import com.example.backend.common.exception.ForbiddenException;
import com.example.backend.common.exception.LockedException;
import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.common.util.Sha256;
import com.example.backend.features.auth.AuthAcciones;
import com.example.backend.features.auth.AuthErrorCodes;
import com.example.backend.features.auth.dto.UsuarioSesionResponse;
import com.example.backend.features.auth.enums.TipoCliente;
import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Rol;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.features.usuarios.repository.UsuarioRolRepository;
import com.example.backend.security.JwtService;
import com.example.backend.security.RlsContext;
import com.example.backend.security.SesionRevocadaRegistry;

/**
 * Lógica de login, renovación, cierre de sesión y "quién soy".
 *
 * * Anti-enumeración: correo inexistente y contraseña incorrecta dan el mismo
 * * error y ambos gastan un BCrypt (hash ficticio para el caso inexistente).
 * * La bitácora usa la clave `correoRef` (no `...Hash`: BitacoraService rechaza
 * * claves con 'hash'). El log de WARN usa solo un hash corto del correo, nunca el correo ni la
 * * contraseña ni tokens.
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
    private static final String MSG_CREDENCIALES = "Correo o contraseña incorrectos";

    private final LoginAttemptService loginAttempts;
    private final SesionService sesionService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RlsContext rlsContext;
    private final BitacoraService bitacora;
    private final SesionRevocadaRegistry revocadas;
    private final String hashFicticio;

    public AuthService(LoginAttemptService loginAttempts, SesionService sesionService, JwtService jwtService,
            PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository,
            UsuarioRolRepository usuarioRolRepository, RlsContext rlsContext, BitacoraService bitacora,
            SesionRevocadaRegistry revocadas) {
        this.revocadas = revocadas;
        this.loginAttempts = loginAttempts;
        this.sesionService = sesionService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.rlsContext = rlsContext;
        this.bitacora = bitacora;
        this.hashFicticio = passwordEncoder.encode("contrasena-ficticia-para-igualar-tiempos");
    }

    /** Datos del request que el controller pasa al service. */
    public record LoginCommand(String email, String password, TipoCliente tipoCliente, String ip,
            String userAgent) {
    }

    /** Resultado interno: el controller decide si los tokens van en cookies o en el body. */
    public record LoginResult(UsuarioSesionResponse usuario, String accessToken, String refreshToken) {
    }

    @Transactional
    public LoginResult login(LoginCommand cmd) {
        String email = cmd.email().trim().toLowerCase(Locale.ROOT);
        String emailHash = Sha256.hex(email).substring(0, 12);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            if (loginAttempts.estaBloqueado(email)) {
                LOG.warn("Login bloqueado por intentos fallidos emailHash={} errorCode={}", emailHash,
                        AuthErrorCodes.ERR_AUTH_02);
                bitacora.registrarEnTransaccionPropia(AuthAcciones.AUTH_CUENTA_BLOQUEADA, AuthAcciones.MODULO,
                        null, null, ResultadoBitacora.FALLO, Map.of("correoRef", emailHash));
                throw new LockedException(AuthErrorCodes.ERR_AUTH_02,
                        "Demasiados intentos fallidos, intentá de nuevo más tarde");
            }

            rlsContext.usarCorreoDeLogin(email);
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
            boolean passwordOk = passwordEncoder.matches(cmd.password(),
                    usuario == null ? hashFicticio : usuario.getPasswordHash());

            if (usuario == null || !passwordOk) {
                loginAttempts.registrarFallo(email);
                LOG.warn("Login fallido emailHash={} errorCode={}", emailHash, AuthErrorCodes.ERR_AUTH_01);
                bitacora.registrarEnTransaccionPropia(AuthAcciones.AUTH_LOGIN_FALLO, AuthAcciones.MODULO, null,
                        null, ResultadoBitacora.FALLO, Map.of("correoRef", emailHash));
                throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_01, MSG_CREDENCIALES);
            }

            if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
                LOG.warn("Login de usuario no activo emailHash={} errorCode={}", emailHash,
                        AuthErrorCodes.ERR_AUTH_03);
                bitacora.registrarEnTransaccionPropia(usuario.getCodigo(), AuthAcciones.AUTH_LOGIN_FALLO,
                        AuthAcciones.MODULO, null, null, ResultadoBitacora.FALLO,
                        Map.of("correoRef", emailHash, "motivo", "USUARIO_NO_ACTIVO"));
                throw new ForbiddenException(AuthErrorCodes.ERR_AUTH_03, "La cuenta no está activa");
            }

            loginAttempts.reiniciar(email);
            rlsContext.usarUsuario(usuario.getCodigo());
            List<String> roles = rolesActivos(usuario);
            SesionService.SesionCreada creada =
                    sesionService.crear(usuario, cmd.tipoCliente(), cmd.ip(), cmd.userAgent());
            String access = jwtService.emitir(usuario.getCodigo(), roles, creada.sesion().getCodigo());

            bitacora.registrar(usuario.getCodigo(), AuthAcciones.AUTH_LOGIN_OK, AuthAcciones.MODULO, "sesion",
                    creada.sesion().getCodigo(), ResultadoBitacora.EXITO, null);
            LOG.info("Login exitoso usuarioCodigo={} tipoCliente={}", usuario.getCodigo(), cmd.tipoCliente());

            return new LoginResult(new UsuarioSesionResponse(usuario.getCodigo(), usuario.getEmail(), roles),
                    access, creada.refreshToken());
        }
    }

    /**
     * Rota el refresh token y emite un access token con los roles de la base.
     *
     * ! `noRollbackFor`: lo que SesionService marca al rechazar (REUSO,
     * ! EXPIRADA) tiene que quedar confirmado. Cualquier otro error (base o
     * ! Redis caídos) deshace todo y no se entrega ningún token.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResult refresh(String refreshToken, String ip, String userAgent) {
        SesionService.SesionCreada rotada = sesionService.rotar(refreshToken, ip, userAgent);
        UUID codigoUsuario = rotada.sesion().getUsuarioCodigo();
        UUID codigoSesion = rotada.sesion().getCodigo();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            rlsContext.usarUsuario(codigoUsuario);
            Usuario usuario = usuarioRepository.findByCodigo(codigoUsuario)
                    .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                    .orElse(null);
            List<String> roles = usuario == null ? List.of() : rolesActivos(usuario);
            // * Falla cerrado: si Redis no responde, la consulta lanza y no se emite nada.
            if (roles.isEmpty() || revocadas.estaRevocada(codigoSesion)) {
                // * Se deshace la rotación: si no, un reintento parecería una reutilización.
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                LOG.warn("Renovación rechazada usuarioCodigo={} errorCode={}", codigoUsuario,
                        AuthErrorCodes.ERR_AUTH_04);
                throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04,
                        "Tu sesión ya no es válida, iniciá sesión de nuevo");
            }

            String access = jwtService.emitir(codigoUsuario, roles, codigoSesion);
            LOG.info("Sesión renovada usuarioCodigo={}", codigoUsuario);
            return new LoginResult(new UsuarioSesionResponse(codigoUsuario, usuario.getEmail(), roles),
                    access, rotada.refreshToken());
        }
    }

    @Transactional
    public void logout(String refreshToken) {
        sesionService.cerrar(refreshToken).ifPresent(sesion -> {
            bitacora.registrar(sesion.getUsuarioCodigo(), AuthAcciones.AUTH_LOGOUT, AuthAcciones.MODULO,
                    "sesion", sesion.getCodigo(), ResultadoBitacora.EXITO, null);
            try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
                LOG.info("Sesión cerrada usuarioCodigo={}", sesion.getUsuarioCodigo());
            }
        });
    }

    @Transactional(readOnly = true)
    public UsuarioSesionResponse me(UUID codigoUsuario) {
        rlsContext.usarUsuario(codigoUsuario);
        Usuario usuario = usuarioRepository.findByCodigo(codigoUsuario)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.ERR_AUTH_07, "No autenticado"));
        return new UsuarioSesionResponse(usuario.getCodigo(), usuario.getEmail(), rolesActivos(usuario));
    }

    private List<String> rolesActivos(Usuario usuario) {
        return usuarioRolRepository.findActivosByUsuario(usuario).stream().map(Rol::getNombre).toList();
    }
}
