package com.example.backend.common.bitacora.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.backend.common.bitacora.enums.ResultadoBitacora;
import com.example.backend.common.bitacora.repository.BitacoraRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Único punto de escritura de la bitácora (ver BITACORA_BACKEND.md).
 *
 * * `registrar` se une a la transacción actual: el evento de un cambio de
 * * negocio vive o muere con ese cambio. `registrarEnTransaccionPropia`
 * * (REQUIRES_NEW) es para fallos y eventos de seguridad que no deben
 * * perderse si la operación hace rollback.
 * * El actor sale del SecurityContext (claim `sub`) o se pasa explícito; sin
 * * ninguno queda NULL (p. ej. un login fallido).
 * ! `registrarEnTransaccionPropia` nunca desde dentro de una transacción en un
 * ! flujo concurrente: pide una segunda conexión y puede agotar el pool.
 * ! `detalle` se sanitiza: nunca claves de secretos y solo valores simples,
 * ! UUID o listas de nombres de campos.
 */
@Service
@RequiredArgsConstructor
public class BitacoraService {

    private static final Logger LOG = LoggerFactory.getLogger(BitacoraService.class);

    private static final int MAX_USER_AGENT = 255;
    private static final int MAX_IP = 45;
    private static final List<String> CLAVES_PROHIBIDAS =
            List.of("password", "token", "secret", "hash", "authorization", "card");

    private final BitacoraRepository bitacoraRepository;

    @Transactional
    public void registrar(String accion, String modulo, String entidad, UUID entidadCodigo,
            ResultadoBitacora resultado, Map<String, Object> detalle) {
        insertar(actorActual(), accion, modulo, entidad, entidadCodigo, resultado, detalle);
    }

    @Transactional
    public void registrar(UUID actor, String accion, String modulo, String entidad, UUID entidadCodigo,
            ResultadoBitacora resultado, Map<String, Object> detalle) {
        insertar(actor, accion, modulo, entidad, entidadCodigo, resultado, detalle);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEnTransaccionPropia(String accion, String modulo, String entidad,
            UUID entidadCodigo, ResultadoBitacora resultado, Map<String, Object> detalle) {
        insertar(actorActual(), accion, modulo, entidad, entidadCodigo, resultado, detalle);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEnTransaccionPropia(UUID actor, String accion, String modulo, String entidad,
            UUID entidadCodigo, ResultadoBitacora resultado, Map<String, Object> detalle) {
        insertar(actor, accion, modulo, entidad, entidadCodigo, resultado, detalle);
    }

    /** Como la anterior, para hilos sin petición en curso: el origen se captura antes. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEnTransaccionPropia(UUID actor, String accion, String modulo, String entidad,
            UUID entidadCodigo, ResultadoBitacora resultado, Map<String, Object> detalle, OrigenSolicitud origen) {
        insertar(actor, accion, modulo, entidad, entidadCodigo, resultado, detalle, origen);
    }

    private void insertar(UUID actor, String accion, String modulo, String entidad, UUID entidadCodigo,
            ResultadoBitacora resultado, Map<String, Object> detalle) {
        HttpServletRequest request = requestActual();
        insertar(actor, accion, modulo, entidad, entidadCodigo, resultado, detalle, request == null
                ? new OrigenSolicitud(null, null)
                : new OrigenSolicitud(request.getRemoteAddr(), request.getHeader("User-Agent")));
    }

    private void insertar(UUID actor, String accion, String modulo, String entidad, UUID entidadCodigo,
            ResultadoBitacora resultado, Map<String, Object> detalle, OrigenSolicitud origen) {
        // Se valida antes de tocar la base: un detalle inválido no inserta nada.
        String detalleJson = serializarDetalle(sanitizar(detalle));
        String ip = truncar(origen.ip(), MAX_IP);
        String userAgent = truncar(origen.userAgent(), MAX_USER_AGENT);

        bitacoraRepository.insertar(UUID.randomUUID(), actor, accion, modulo, entidad, entidadCodigo,
                resultado.name(), ip, userAgent, detalleJson);
        LOG.debug("Evento de bitácora registrado: {} / {}", modulo, accion);
    }

    private static UUID actorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static HttpServletRequest requestActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }

    /**
     * Rechaza claves que sugieran secretos y valores que no sean simples,
     * UUID o listas de nombres de campos.
     */
    static Map<String, Object> sanitizar(Map<String, Object> detalle) {
        if (detalle == null) {
            return null;
        }
        for (Map.Entry<String, Object> entrada : detalle.entrySet()) {
            String clave = entrada.getKey() == null ? "" : entrada.getKey().toLowerCase(Locale.ROOT);
            for (String prohibida : CLAVES_PROHIBIDAS) {
                if (clave.contains(prohibida)) {
                    throw new IllegalArgumentException(
                            "La clave '" + entrada.getKey() + "' no se permite en el detalle de bitácora");
                }
            }
            if (!valorPermitido(entrada.getValue())) {
                throw new IllegalArgumentException(
                        "Valor no permitido en el detalle de bitácora para la clave '" + entrada.getKey() + "'");
            }
        }
        return detalle;
    }

    private static boolean valorPermitido(Object valor) {
        if (valor == null || valor instanceof String || valor instanceof Number
                || valor instanceof Boolean || valor instanceof UUID) {
            return true;
        }
        if (valor instanceof List<?> lista) {
            return lista.stream().allMatch(String.class::isInstance);
        }
        return false;
    }

    /**
     * JSON mínimo para los tipos que `sanitizar` permite. Se escribe a mano
     * para no depender de la versión de Jackson que traiga Spring Boot.
     */
    static String serializarDetalle(Map<String, Object> detalle) {
        if (detalle == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("{");
        boolean primero = true;
        for (Map.Entry<String, Object> entrada : detalle.entrySet()) {
            if (!primero) {
                sb.append(',');
            }
            primero = false;
            sb.append(texto(entrada.getKey())).append(':').append(valor(entrada.getValue()));
        }
        return sb.append('}').toString();
    }

    private static String valor(Object valor) {
        if (valor == null) {
            return "null";
        }
        if (valor instanceof Number || valor instanceof Boolean) {
            return valor.toString();
        }
        if (valor instanceof List<?> lista) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(texto(lista.get(i).toString()));
            }
            return sb.append(']').toString();
        }
        return texto(valor.toString());
    }

    private static String texto(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
