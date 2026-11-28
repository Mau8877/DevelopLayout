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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.bitacora.service.BitacoraService;
import com.example.backend.common.bitacora.enums.ResultadoBitacora;
import com.example.backend.common.exception.ForbiddenException;
import com.example.backend.common.exception.LockedException;
import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.common.util.Sha256;
import com.example.backend.features.auth.constant.AuthAcciones;
import com.example.backend.features.auth.constant.AuthErrorCodes;
import com.example.backend.features.auth.dto.UsuarioSesionResponse;
import com.example.backend.features.auth.enums.TipoCliente;
import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Rol;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.features.usuarios.repository.UsuarioRolRepository;
import com.example.backend.security.jwt.JwtService;
import com.example.backend.security.RlsContext;
import com.example.backend.security.jwt.SesionRevocadaRegistry;

/**
 * Lógica de login, renovación, cierre de sesión y "quién soy".
 *
 * * Anti-enumeración: correo inexistente y contraseña incorrecta dan el mismo
 * * error y ambos gastan un BCrypt (hash ficticio para el caso inexistente).
 * ! `login` NO es @Transactional a propósito: ninguna conexión queda tomada
 * ! mientras corre BCrypt, y nunca se pide una segunda (la bitácora de un fallo
 * ! usa su propia transacción) teniendo ya una. Con eso, 10 fallos simultáneos
 * ! agotaban el pool.
 * * La bitácora usa la clave `correoRef` (no `...Hash`: BitacoraService rechaza
 * * claves con 'hash'). El log de WARN usa solo un hash corto del correo, nunca el correo ni la
 * * contraseña ni tokens.
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
    private static final String MSG_CREDENCIALES = "auth.error.credenciales";

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
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;

    public AuthService(LoginAttemptService loginAttempts, SesionService sesionService, JwtService jwtService,
            PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository,
            UsuarioRolRepository usuarioRolRepository, RlsContext rlsContext, BitacoraService bitacora,
            SesionRevocadaRegistry revocadas, PlatformTransactionManager transacciones) {
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
        this.escritura = new TransactionTemplate(transacciones);
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

    public LoginResult login(LoginCommand cmd) {
        String email = cmd.email().trim().toLowerCase(Locale.ROOT);
        String emailHash = Sha256.hex(email).substring(0, 12);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
            LoginAttemptService.Reserva reserva = loginAttempts.reservar(email, cmd.ip());
            if (reserva != LoginAttemptService.Reserva.PERMITIDO) {
                boolean porCuenta = reserva == LoginAttemptService.Reserva.BLOQUEADO_CUENTA;
                String alcance = porCuenta ? "cuenta" : "origen";
                // * El bloqueo por cuenta es la señal de un ataque repartido: sirve de alerta.
                LOG.warn("{} emailHash={} errorCode={}", porCuenta
                        ? "ALERTA: tope de intentos de la cuenta alcanzado desde varias IP"
                        : "Login bloqueado por intentos fallidos", emailHash, AuthErrorCodes.ERR_AUTH_02);
                bitacora.registrarEnTransaccionPropia(AuthAcciones.AUTH_CUENTA_BLOQUEADA, AuthAcciones.MODULO,
                        null, null, ResultadoBitacora.FALLO, Map.of("correoRef", emailHash, "alcance", alcance));
                throw new LockedException(AuthErrorCodes.ERR_AUTH_02,
                        "auth.error.bloqueado");
            }

            Usuario usuario = lectura.execute(estado -> {
                rlsContext.usarCorreoDeLogin(email);
                return usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
            });
            boolean passwordOk = passwordEncoder.matches(cmd.password(),
                    usuario == null ? hashFicticio : usuario.getPasswordHash());

            if (usuario == null || !passwordOk) {
                LOG.warn("Login fallido emailHash={} errorCode={}", emailHash, AuthErrorCodes.ERR_AUTH_01);
                bitacora.registrarEnTransaccionPropia(AuthAcciones.AUTH_LOGIN_FALLO, AuthAcciones.MODULO, null,
                        null, ResultadoBitacora.FALLO, Map.of("correoRef", emailHash));
                throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_01, MSG_CREDENCIALES);
            }

            if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
                loginAttempts.liberar(email, cmd.ip());
                LOG.warn("Login de usuario no activo emailHash={} errorCode={}", emailHash,
                        AuthErrorCodes.ERR_AUTH_03);
                bitacora.registrarEnTransaccionPropia(usuario.getCodigo(), AuthAcciones.AUTH_LOGIN_FALLO,
                        AuthAcciones.MODULO, null, null, ResultadoBitacora.FALLO,
                        Map.of("correoRef", emailHash, "motivo", "USUARIO_NO_ACTIVO"));
                throw new ForbiddenException(AuthErrorCodes.ERR_AUTH_03, "auth.error.cuentaInactiva");
            }

            LoginResult resultado =
                    escritura.execute(estado -> abrirSesion(usuario.getCodigo(), usuario.getPasswordHash(), cmd));
            loginAttempts.confirmarExito(email, cmd.ip());
            LOG.info("Login exitoso usuarioCodigo={} tipoCliente={}", usuario.getCodigo(), cmd.tipoCliente());
            return resultado;
        }
    }

    // * Se relee el usuario dentro de la transacción que escribe: pudo cambiar
    // * mientras se comparaba la contraseña.
    // ! El bloqueo compartido y la comparación del hash impiden que un login con la
    // ! contraseña anterior cree una sesión después de un restablecimiento.
    private LoginResult abrirSesion(UUID codigoUsuario, String hashVerificado, LoginCommand cmd) {
        rlsContext.usarUsuario(codigoUsuario);
        Usuario usuario = usuarioRepository.bloquearParaLogin(codigoUsuario)
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO && u.getPasswordHash().equals(hashVerificado))
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.ERR_AUTH_01, MSG_CREDENCIALES));
        List<String> roles = rolesActivos(usuario);
        SesionService.SesionCreada creada =
                sesionService.crear(usuario, cmd.tipoCliente(), cmd.ip(), cmd.userAgent());
        String access = jwtService.emitir(codigoUsuario, roles, creada.sesion().getCodigo());

        bitacora.registrar(codigoUsuario, AuthAcciones.AUTH_LOGIN_OK, AuthAcciones.MODULO, "sesion",
                creada.sesion().getCodigo(), ResultadoBitacora.EXITO, null);

        return new LoginResult(new UsuarioSesionResponse(codigoUsuario, usuario.getEmail(), roles),
                access, creada.refreshToken());
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
                        "auth.error.sesionInvalida");
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
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.ERR_AUTH_07, "common.error.noAutenticado"));
        return new UsuarioSesionResponse(usuario.getCodigo(), usuario.getEmail(), rolesActivos(usuario));
    }

    private List<String> rolesActivos(Usuario usuario) {
        return usuarioRolRepository.findActivosByUsuario(usuario).stream().map(Rol::getNombre).toList();
    }
}
