package com.example.backend.features.usuarios.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.bitacora.service.BitacoraService;
import com.example.backend.common.bitacora.enums.ResultadoBitacora;
import com.example.backend.common.exception.ConflictException;
import com.example.backend.common.exception.NotFoundException;
import com.example.backend.common.exception.ValidationException;
import com.example.backend.features.auth.enums.MotivoRevocacion;
import com.example.backend.features.auth.service.InvitacionEnvio;
import com.example.backend.features.auth.service.InvitacionTokenService;
import com.example.backend.features.auth.service.PasswordResetTokenService;
import com.example.backend.features.auth.service.SesionService;
import com.example.backend.features.usuarios.constant.RolNombres;
import com.example.backend.features.usuarios.constant.UsuarioAcciones;
import com.example.backend.features.usuarios.constant.UsuarioErrorCodes;
import com.example.backend.features.usuarios.dto.ReenvioInvitacionResponse;
import com.example.backend.features.usuarios.dto.UsuarioCreadoResponse;
import com.example.backend.features.usuarios.dto.UsuarioResponse;
import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Rol;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.model.UsuarioRol;
import com.example.backend.features.usuarios.repository.RolRepository;
import com.example.backend.features.usuarios.repository.UsuarioBusquedaRepository;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.features.usuarios.repository.UsuarioRolRepository;
import com.example.backend.security.RlsContext;

/** Administración de usuarios. */
@Service
public class UsuarioAdminService {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioAdminService.class);
    private static final String ES_ADMIN = "hasRole('SUPER_ADMIN')";
    private static final int BYTES_PASSWORD_DESCARTADA = 32;

    private final UsuarioBusquedaRepository busqueda;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;
    private final GuardiaAdministradores guardia;
    private final InvitacionEnvio invitaciones;
    private final LimiteReenvios limiteReenvios;
    private final SesionService sesionService;
    private final InvitacionTokenService tokensInvitacion;
    private final PasswordResetTokenService tokensRecuperacion;
    private final PasswordEncoder passwordEncoder;
    private final BitacoraService bitacora;
    private final RlsContext rlsContext;
    private final TransactionTemplate transaccion;
    private final SecureRandom azar = new SecureRandom();

    public UsuarioAdminService(UsuarioBusquedaRepository busqueda, UsuarioRepository usuarioRepository,
            UsuarioRolRepository usuarioRolRepository, RolRepository rolRepository, GuardiaAdministradores guardia,
            InvitacionEnvio invitaciones, LimiteReenvios limiteReenvios, SesionService sesionService,
            InvitacionTokenService tokensInvitacion, PasswordResetTokenService tokensRecuperacion,
            PasswordEncoder passwordEncoder, BitacoraService bitacora, RlsContext rlsContext,
            PlatformTransactionManager transacciones) {
        this.busqueda = busqueda;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.rolRepository = rolRepository;
        this.guardia = guardia;
        this.invitaciones = invitaciones;
        this.limiteReenvios = limiteReenvios;
        this.sesionService = sesionService;
        this.tokensInvitacion = tokensInvitacion;
        this.tokensRecuperacion = tokensRecuperacion;
        this.passwordEncoder = passwordEncoder;
        this.bitacora = bitacora;
        this.rlsContext = rlsContext;
        this.transaccion = new TransactionTemplate(transacciones);
    }

    /** Quién pide un cambio y desde qué sesión. */
    public record Actor(UUID codigo, UUID sesion) {
    }

    /** Usuarios que cumplen todos los criterios, el alta más reciente primero. */
    @PreAuthorize(ES_ADMIN)
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> buscar(UsuarioBusquedaRepository.Filtro criterios, Pageable pageable) {
        rlsContext.usarAdmin();
        Page<Usuario> pagina = busqueda.buscar(criterios, pageable);
        Map<Long, List<String>> roles = pagina.isEmpty() ? Map.of() : rolesDe(pagina.getContent());
        return pagina.map(u -> aRespuesta(u, roles.getOrDefault(u.getId(), List.of())));
    }

    /**
     * Da de alta una cuenta con la invitación pendiente y envía el correo.
     *
     * ! No es @Transactional: BCrypt corre antes y el envío después, sin
     * ! conexión tomada. Un correo que no sale no deshace la cuenta.
     */
    @PreAuthorize(ES_ADMIN)
    public UsuarioCreadoResponse crear(Actor actor, String emailRecibido, List<String> rolesPedidos) {
        String email = emailRecibido.trim().toLowerCase(Locale.ROOT);
        String hashInutilizable = passwordEncoder.encode(passwordDescartada());
        UsuarioResponse creado;
        try {
            creado = transaccion.execute(estado -> guardarCuenta(actor, email, hashInutilizable, rolesPedidos));
        } catch (DataIntegrityViolationException e) {
            // * Respaldo del índice único: los altas ya van de a una por la guardia.
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_usuario_email")) {
                throw correoRepetido();
            }
            throw e;
        }
        boolean enviada = invitaciones.enviar(creado.codigo(), creado.email());
        return new UsuarioCreadoResponse(creado, enviada);
    }

    /**
     * Envía una invitación nueva, que anula la anterior.
     *
     * * Si la persona acepta entre la comprobación y el envío, el enlace nuevo no
     * * sirve: la aceptación exige que la invitación siga pendiente.
     */
    @PreAuthorize(ES_ADMIN)
    public ReenvioInvitacionResponse reenviarInvitacion(Actor actor, UUID codigo) {
        String email = transaccion.execute(estado -> {
            guardia.exigirVigente(actor.codigo(), actor.sesion());
            Usuario usuario = usuarioRepository.findByCodigo(codigo).orElseThrow(UsuarioAdminService::noExiste);
            if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
                throw new ConflictException(UsuarioErrorCodes.ERR_US_05,
                        "usuarios.error.cuentaInactiva");
            }
            if (!usuario.isInvitacionPendiente()) {
                throw new ConflictException(UsuarioErrorCodes.ERR_US_05, "usuarios.error.invitacionAceptada");
            }
            return usuario.getEmail();
        });
        if (!limiteReenvios.reservar(codigo)) {
            throw new ConflictException(UsuarioErrorCodes.ERR_US_06,
                    "usuarios.error.limiteReenvios");
        }
        boolean enviada = invitaciones.enviar(codigo, email);
        bitacora.registrarEnTransaccionPropia(actor.codigo(), UsuarioAcciones.USUARIOS_INVITACION_REENVIADA,
                UsuarioAcciones.MODULO, "usuario", codigo,
                enviada ? ResultadoBitacora.EXITO : ResultadoBitacora.FALLO, null);
        return new ReenvioInvitacionResponse(enviada);
    }

    /**
     * Desactiva la cuenta: cierra sus sesiones y anula sus enlaces. Repetirlo no
     * hace nada.
     *
     * * El bloqueo de la fila hace que un login en curso termine antes (y su
     * * sesión se revoque acá) o espere y vea la cuenta inactiva.
     * ! Si Redis falla, no se confirma nada.
     */
    @PreAuthorize(ES_ADMIN)
    @Transactional
    public UsuarioResponse desactivar(Actor actor, UUID codigo) {
        guardia.exigirVigente(actor.codigo(), actor.sesion());
        if (codigo.equals(actor.codigo())) {
            throw new ConflictException(UsuarioErrorCodes.ERR_US_04, "usuarios.error.propiaCuenta");
        }
        Usuario usuario = usuarioRepository.bloquearParaCambio(codigo).orElseThrow(UsuarioAdminService::noExiste);
        if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
            usuario.desactivar();
            sesionService.revocarTodas(codigo, MotivoRevocacion.ADMIN);
            anularEnlaces(codigo);
            registrarCambioDeEstado(actor, codigo, UsuarioAcciones.USUARIOS_USUARIO_DESACTIVADO);
        }
        return aRespuesta(usuario, nombresDeRoles(usuario));
    }

    /** Vuelve a permitir el acceso. No recupera sesiones ni enlaces. Repetirlo no hace nada. */
    @PreAuthorize(ES_ADMIN)
    @Transactional
    public UsuarioResponse reactivar(Actor actor, UUID codigo) {
        guardia.exigirVigente(actor.codigo(), actor.sesion());
        Usuario usuario = usuarioRepository.bloquearParaCambio(codigo).orElseThrow(UsuarioAdminService::noExiste);
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            // * Otra vez: una recuperación en curso pudo emitir un enlace justo al desactivar.
            anularEnlaces(codigo);
            usuario.reactivar();
            registrarCambioDeEstado(actor, codigo, UsuarioAcciones.USUARIOS_USUARIO_REACTIVADO);
        }
        return aRespuesta(usuario, nombresDeRoles(usuario));
    }

    /**
     * Deja a la cuenta exactamente con esos roles y cierra todas sus sesiones, para
     * que rijan desde ya. Pedir el mismo conjunto no hace nada.
     *
     * * El bloqueo de la fila hace que un login en curso espere y lea los roles
     * * nuevos, o termine antes y su sesión se revoque acá.
     * ! Si Redis falla, no se confirma nada.
     */
    @PreAuthorize(ES_ADMIN)
    @Transactional
    public UsuarioResponse cambiarRoles(Actor actor, UUID codigo, List<String> rolesPedidos) {
        guardia.exigirVigente(actor.codigo(), actor.sesion());
        List<Rol> pedidos = rolesExistentes(rolesPedidos);
        Usuario usuario = usuarioRepository.bloquearParaCambio(codigo).orElseThrow(UsuarioAdminService::noExiste);

        List<UsuarioRol> asignaciones = usuarioRolRepository.findTodasConRol(usuario);
        // * Una asignación a un rol desactivado en el catálogo no cuenta ni se toca.
        Set<String> actuales = asignaciones.stream()
                .filter(a -> a.isActive() && a.getRol().isActive())
                .map(a -> a.getRol().getNombre())
                .collect(Collectors.toSet());
        Set<String> nuevos = pedidos.stream().map(Rol::getNombre).collect(Collectors.toSet());
        List<String> agregados = diferencia(nuevos, actuales);
        List<String> quitados = diferencia(actuales, nuevos);
        if (agregados.isEmpty() && quitados.isEmpty()) {
            return aRespuesta(usuario, List.copyOf(actuales));
        }
        if (codigo.equals(actor.codigo()) && quitados.contains(RolNombres.SUPER_ADMIN)) {
            throw new ConflictException(UsuarioErrorCodes.ERR_US_07,
                    "usuarios.error.rolPropio");
        }

        aplicar(usuario, asignaciones, pedidos, agregados, quitados);
        sesionService.revocarTodas(codigo, MotivoRevocacion.ADMIN);
        bitacora.registrar(actor.codigo(), UsuarioAcciones.USUARIOS_ROLES_EDITADOS, UsuarioAcciones.MODULO,
                "usuario", codigo, ResultadoBitacora.EXITO, Map.of("agregados", agregados, "quitados", quitados));
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "usuarios")) {
            LOG.info("Roles cambiados usuarioCodigo={} actor={}", codigo, actor.codigo());
        }
        return aRespuesta(usuario, List.copyOf(nuevos));
    }

    private void aplicar(Usuario usuario, List<UsuarioRol> asignaciones, List<Rol> pedidos,
            List<String> agregados, List<String> quitados) {
        asignaciones.stream()
                .filter(a -> quitados.contains(a.getRol().getNombre()))
                .forEach(UsuarioRol::desactivar);
        for (Rol rol : pedidos) {
            if (agregados.contains(rol.getNombre())) {
                asignaciones.stream()
                        .filter(a -> a.getRol().getNombre().equals(rol.getNombre()))
                        .findFirst()
                        .ifPresentOrElse(UsuarioRol::activar,
                                () -> usuarioRolRepository.save(new UsuarioRol(usuario, rol)));
            }
        }
    }

    private static List<String> diferencia(Set<String> conjunto, Set<String> menos) {
        return conjunto.stream().filter(nombre -> !menos.contains(nombre)).sorted().toList();
    }

    private void anularEnlaces(UUID usuario) {
        tokensInvitacion.anular(usuario);
        tokensRecuperacion.anular(usuario);
    }

    private void registrarCambioDeEstado(Actor actor, UUID codigo, String accion) {
        bitacora.registrar(actor.codigo(), accion, UsuarioAcciones.MODULO, "usuario", codigo,
                ResultadoBitacora.EXITO, null);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "usuarios")) {
            LOG.info("Estado de usuario cambiado accion={} usuarioCodigo={} actor={}", accion, codigo,
                    actor.codigo());
        }
    }

    private List<String> nombresDeRoles(Usuario usuario) {
        return usuarioRolRepository.findActivosByUsuario(usuario).stream().map(Rol::getNombre).toList();
    }

    private UsuarioResponse guardarCuenta(Actor actor, String email, String hash, List<String> rolesPedidos) {
        guardia.exigirVigente(actor.codigo(), actor.sesion());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw correoRepetido();
        }
        List<Rol> roles = rolesExistentes(rolesPedidos);
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.invitado(email, hash));
        roles.forEach(rol -> usuarioRolRepository.save(new UsuarioRol(usuario, rol)));
        usuarioRolRepository.flush();

        List<String> nombres = roles.stream().map(Rol::getNombre).sorted().toList();
        bitacora.registrar(actor.codigo(), UsuarioAcciones.USUARIOS_USUARIO_CREADO, UsuarioAcciones.MODULO,
                "usuario", usuario.getCodigo(), ResultadoBitacora.EXITO, Map.of("roles", nombres));
        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "usuarios")) {
            LOG.info("Usuario creado usuarioCodigo={} actor={}", usuario.getCodigo(), actor.codigo());
        }
        return aRespuesta(usuario, nombres);
    }

    private List<Rol> rolesExistentes(List<String> pedidos) {
        return pedidos.stream().distinct()
                .map(nombre -> rolRepository.findByNombre(nombre)
                        .filter(Rol::isActive)
                        .orElseThrow(() -> new ValidationException(UsuarioErrorCodes.ERR_US_03,
                                "usuarios.error.rolInexistente")))
                .toList();
    }

    private String passwordDescartada() {
        byte[] bytes = new byte[BYTES_PASSWORD_DESCARTADA];
        azar.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static ConflictException correoRepetido() {
        return new ConflictException(UsuarioErrorCodes.ERR_US_02,
                "usuarios.error.correoRepetido");
    }

    private static NotFoundException noExiste() {
        return new NotFoundException(UsuarioErrorCodes.ERR_US_01, "usuarios.error.noExiste");
    }

    private Map<Long, List<String>> rolesDe(List<Usuario> usuarios) {
        return usuarioRolRepository.findActivasConRol(usuarios).stream()
                .collect(Collectors.groupingBy(a -> a.getUsuario().getId(),
                        Collectors.mapping(a -> a.getRol().getNombre(), Collectors.toList())));
    }

    private static UsuarioResponse aRespuesta(Usuario u, List<String> roles) {
        return new UsuarioResponse(u.getCodigo(), u.getEmail(), roles.stream().sorted().toList(), u.getEstado(),
                u.isInvitacionPendiente(), u.getCreatedAt());
    }
}
