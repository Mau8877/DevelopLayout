package com.example.backend.features.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.bitacora.BitacoraService;
import com.example.backend.common.bitacora.ResultadoBitacora;
import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.common.util.Sha256;
import com.example.backend.features.auth.AuthAcciones;
import com.example.backend.features.auth.AuthErrorCodes;
import com.example.backend.features.auth.enums.EstadoSesion;
import com.example.backend.features.auth.enums.MotivoRevocacion;
import com.example.backend.features.auth.enums.TipoCliente;
import com.example.backend.features.auth.model.Sesion;
import com.example.backend.features.auth.repository.SesionRepository;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.security.RlsContext;
import com.example.backend.security.SesionRevocadaRegistry;

/**
 * Ciclo de vida de las sesiones: crear, rotar el refresh token, revocar y cerrar.
 *
 * * El token en claro (256 bits) solo se devuelve al llamador; en la base
 * * queda únicamente su SHA-256.
 * ! `crear` debe correr dentro de la transacción del login, con
 * ! `app.current_user` ya fijado (RLS exige que la fila sea del usuario).
 */
@Service
public class SesionService {

    private static final Logger LOG = LoggerFactory.getLogger(SesionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SesionRepository sesionRepository;
    private final SesionRevocadaRegistry revocadas;
    private final RlsContext rlsContext;
    private final BitacoraService bitacora;
    private final Duration refreshTtl;
    private final Duration vidaMaxima;
    private final Duration accessTtl;

    public SesionService(SesionRepository sesionRepository, SesionRevocadaRegistry revocadas,
            RlsContext rlsContext, BitacoraService bitacora,
            @Value("${app.auth.refresh-ttl:P7D}") Duration refreshTtl,
            @Value("${app.auth.session-max-lifetime:P30D}") Duration vidaMaxima,
            @Value("${app.jwt.access-ttl:PT15M}") Duration accessTtl) {
        this.sesionRepository = sesionRepository;
        this.revocadas = revocadas;
        this.rlsContext = rlsContext;
        this.bitacora = bitacora;
        this.refreshTtl = refreshTtl;
        this.vidaMaxima = vidaMaxima;
        this.accessTtl = accessTtl;
    }

    public SesionCreada crear(Usuario usuario, TipoCliente tipo, String ip, String userAgent) {
        String token = nuevoToken();

        Instant ahora = Instant.now();
        Sesion sesion = new Sesion(usuario.getId(), usuario.getCodigo(), Sha256.hex(token), tipo,
                truncar(ip, 45), truncar(userAgent, 255), ahora.plus(refreshTtl));
        sesion.setUltimoUsoEn(ahora);
        return new SesionCreada(sesionRepository.save(sesion), token);
    }

    /**
     * Canjea un refresh token por uno nuevo en la misma sesión.
     *
     * * La fila se lee con bloqueo: de dos canjes simultáneos, el segundo ve el
     * * token ya rotado.
     * ! `noRollbackFor`: la revocación por reutilización y la expiración tienen
     * ! que confirmarse aunque se responda 401.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public SesionCreada rotar(String tokenEnClaro, String ip, String userAgent) {
        if (tokenEnClaro == null || tokenEnClaro.isBlank()) {
            throw sesionInvalida();
        }
        String hash = Sha256.hex(tokenEnClaro);
        rlsContext.usarRefreshHash(hash);

        Optional<Sesion> actual = sesionRepository.bloquearPorRefreshHash(hash);
        if (actual.isEmpty()) {
            detectarReutilizacion(hash);
            throw sesionInvalida();
        }

        Sesion sesion = actual.get();
        // * Con solo el hash, RLS deja leer la fila pero no actualizarla.
        rlsContext.usarUsuario(sesion.getUsuarioCodigo());
        if (sesion.getEstado() != EstadoSesion.ACTIVA) {
            throw sesionInvalida();
        }

        Instant ahora = Instant.now();
        Instant limite = sesion.getCreatedAt().plus(vidaMaxima);
        if (!ahora.isBefore(sesion.getExpiraEn()) || !ahora.isBefore(limite)) {
            sesion.setEstado(EstadoSesion.EXPIRADA);
            throw sesionInvalida();
        }

        String token = nuevoToken();
        Instant porInactividad = ahora.plus(refreshTtl);
        sesion.setRefreshHashAnterior(sesion.getRefreshHash());
        sesion.setRefreshHash(Sha256.hex(token));
        sesion.setUltimoUsoEn(ahora);
        sesion.setIp(truncar(ip, 45));
        sesion.setExpiraEn(porInactividad.isBefore(limite) ? porInactividad : limite);
        return new SesionCreada(sesion, token);
    }

    /**
     * Revoca la sesión y corta de inmediato su access token. Idempotente.
     *
     * ! Si Redis falla, el error se propaga y la revocación no se confirma.
     */
    public void revocar(Sesion sesion, MotivoRevocacion motivo) {
        if (sesion.getEstado() != EstadoSesion.ACTIVA) {
            return;
        }
        sesion.setEstado(EstadoSesion.REVOCADA);
        sesion.setMotivoRevocacion(motivo);
        sesion.setRevocadaEn(Instant.now());
        revocadas.revocar(sesion.getCodigo(), accessTtl);
    }

    /**
     * Cierra la sesión dueña de este refresh token. Idempotente: un token
     * desconocido o de una sesión ya cerrada no hace nada.
     *
     * @return la sesión que se cerró en esta llamada, si hubo alguna.
     */
    @Transactional
    public Optional<Sesion> cerrar(String tokenEnClaro) {
        if (tokenEnClaro == null || tokenEnClaro.isBlank()) {
            return Optional.empty();
        }
        String hash = Sha256.hex(tokenEnClaro);
        rlsContext.usarRefreshHash(hash);
        Optional<Sesion> activa = sesionRepository.bloquearPorRefreshHash(hash)
                .filter(s -> s.getEstado() == EstadoSesion.ACTIVA);
        activa.ifPresent(sesion -> {
            rlsContext.usarUsuario(sesion.getUsuarioCodigo());
            revocar(sesion, MotivoRevocacion.LOGOUT);
        });
        return activa;
    }

    // * Un token ya rotado que vuelve a aparecer se asume robado.
    private void detectarReutilizacion(String hash) {
        sesionRepository.bloquearPorRefreshHashAnterior(hash)
                .filter(s -> s.getEstado() == EstadoSesion.ACTIVA)
                .ifPresent(sesion -> {
                    rlsContext.usarUsuario(sesion.getUsuarioCodigo());
                    revocar(sesion, MotivoRevocacion.REUSO);
                    bitacora.registrarEnTransaccionPropia(sesion.getUsuarioCodigo(),
                            AuthAcciones.AUTH_REFRESH_REUSO, AuthAcciones.MODULO, "sesion", sesion.getCodigo(),
                            ResultadoBitacora.FALLO, Map.of());
                    try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "auth")) {
                        LOG.warn("Refresh token reutilizado, sesión revocada sesionCodigo={} errorCode={}",
                                sesion.getCodigo(), AuthErrorCodes.ERR_AUTH_04);
                    }
                });
    }

    private static UnauthorizedException sesionInvalida() {
        return new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04,
                "Tu sesión ya no es válida, iniciá sesión de nuevo");
    }

    private static String nuevoToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String truncar(String valor, int max) {
        return valor == null || valor.length() <= max ? valor : valor.substring(0, max);
    }

    /** El `refreshToken` es el valor en claro; no se loguea ni se persiste. */
    public record SesionCreada(Sesion sesion, String refreshToken) {
    }
}
