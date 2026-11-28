package com.example.backend.security;

import com.example.backend.common.idioma.Mensajes;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limit por IP sobre /api/v1/** (endpoints de negocio).
 *
 * * Las entradas se liberan tras un rato sin uso (cuando el bucket ya se rellenó,
 * * borrarlo no cambia nada) y hay un máximo de clientes en memoria. Al llegar al
 * * máximo se desaloja alguna, que recupera su cupo antes de tiempo: es el
 * * precio de que la memoria tenga techo.
 * * En memoria (caché de Caffeine), no distribuido -- correcto
 * * para una sola instancia de backend (el caso de este proyecto hoy). Si
 * * en algún momento se corre más de una réplica del backend, cada una
 * * tendría su propio límite independiente en vez de uno compartido; para
 * * eso hay que migrar a bucket4j_jdk11-redis (Redis ya está en el stack
 * * y spring-boot-starter-data-redis ya está en el pom.xml).
 * ! No aplica a /health, /prometheus ni /swagger-ui/** -- ver shouldNotFilter.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration INACTIVIDAD_MINIMA = Duration.ofMinutes(10);

    // ! Las rutas se comparan como las compara Spring MVC (decodificadas). Con el texto
    // ! crudo de la URL, `/auth/lo%67in` llegaba al login sin pasar por ningún límite.
    private static final PathPatternRequestMatcher.Builder RUTAS = PathPatternRequestMatcher.withDefaults();
    private static final RequestMatcher API = RUTAS.matcher("/api/v1/**");
    private static final RequestMatcher LOGIN = post("login");
    private static final RequestMatcher REFRESH = post("refresh");
    private static final RequestMatcher FORGOT = post("password/forgot");
    private static final RequestMatcher RESET = post("password/reset");
    private static final RequestMatcher ACEPTAR = post("invitation/accept");
    private static final RequestMatcher CAMBIAR = post("password/change");

    private final Cache<String, Bucket> buckets;

    private final Mensajes mensajes;

    // * Límite más estricto, por IP, solo para el login (además del global).
    private final Cache<String, Bucket> loginBuckets;

    // * /refresh no exige access token: lleva su propio límite por IP.
    private final Cache<String, Bucket> refreshBuckets;

    // * Recuperación de contraseña: sin sesión, cada paso con su límite por IP.
    private final Cache<String, Bucket> forgotBuckets;
    private final Cache<String, Bucket> resetBuckets;

    // * Cambio de contraseña con sesión: además del límite por cuenta.
    private final Cache<String, Bucket> changeBuckets;

    private final int changeCapacity;
    private final int changeRefillPerMinute;
    private final int forgotCapacity;
    private final int forgotRefillPerMinute;
    private final int resetCapacity;
    private final int resetRefillPerMinute;
    private final int capacity;
    private final int refillPerMinute;
    private final int loginCapacity;
    private final int loginRefillPerMinute;
    private final int refreshCapacity;
    private final int refreshRefillPerMinute;

    @Autowired
    public RateLimitFilter(
            @Value("${app.ratelimit.capacity}") int capacity,
            @Value("${app.ratelimit.refill-per-minute}") int refillPerMinute,
            @Value("${app.ratelimit.login-capacity:10}") int loginCapacity,
            @Value("${app.ratelimit.login-refill-per-minute:10}") int loginRefillPerMinute,
            @Value("${app.ratelimit.refresh-capacity:30}") int refreshCapacity,
            @Value("${app.ratelimit.refresh-refill-per-minute:30}") int refreshRefillPerMinute,
            @Value("${app.ratelimit.forgot-capacity:5}") int forgotCapacity,
            @Value("${app.ratelimit.forgot-refill-per-minute:1}") int forgotRefillPerMinute,
            @Value("${app.ratelimit.reset-capacity:10}") int resetCapacity,
            @Value("${app.ratelimit.reset-refill-per-minute:5}") int resetRefillPerMinute,
            @Value("${app.ratelimit.max-clients:100000}") int maxClientes,
            @Value("${app.ratelimit.change-capacity:10}") int changeCapacity,
            @Value("${app.ratelimit.change-refill-per-minute:5}") int changeRefillPerMinute, Mensajes mensajes) {
        this(capacity, refillPerMinute, loginCapacity, loginRefillPerMinute, refreshCapacity,
                refreshRefillPerMinute, forgotCapacity, forgotRefillPerMinute, resetCapacity,
                resetRefillPerMinute, maxClientes, Ticker.systemTicker(), changeCapacity, changeRefillPerMinute,
                mensajes);
    }

    /** Para las pruebas: reloj del sistema y mensajes por defecto. */
    RateLimitFilter(int capacity, int refillPerMinute, int loginCapacity, int loginRefillPerMinute,
            int refreshCapacity, int refreshRefillPerMinute, int forgotCapacity, int forgotRefillPerMinute,
            int resetCapacity, int resetRefillPerMinute, int maxClientes) {
        this(capacity, refillPerMinute, loginCapacity, loginRefillPerMinute, refreshCapacity, refreshRefillPerMinute,
                forgotCapacity, forgotRefillPerMinute, resetCapacity, resetRefillPerMinute, maxClientes,
                Ticker.systemTicker());
    }

    RateLimitFilter(int capacity, int refillPerMinute, int loginCapacity, int loginRefillPerMinute,
            int refreshCapacity, int refreshRefillPerMinute, int forgotCapacity, int forgotRefillPerMinute,
            int resetCapacity, int resetRefillPerMinute, int maxClientes, Ticker reloj) {
        this(capacity, refillPerMinute, loginCapacity, loginRefillPerMinute, refreshCapacity, refreshRefillPerMinute,
                forgotCapacity, forgotRefillPerMinute, resetCapacity, resetRefillPerMinute, maxClientes, reloj,
                10, 5, new Mensajes(Mensajes.crearFuente()));
    }

    private RateLimitFilter(int capacity, int refillPerMinute, int loginCapacity, int loginRefillPerMinute,
            int refreshCapacity, int refreshRefillPerMinute, int forgotCapacity, int forgotRefillPerMinute,
            int resetCapacity, int resetRefillPerMinute, int maxClientes, Ticker reloj, int changeCapacity,
            int changeRefillPerMinute, Mensajes mensajes) {
        this.mensajes = mensajes;
        if (maxClientes < 1) {
            throw new IllegalStateException("app.ratelimit.max-clients debe ser 1 o más");
        }
        this.buckets = nuevaCache(inactividad(capacity, refillPerMinute), maxClientes, reloj);
        this.loginBuckets = nuevaCache(inactividad(loginCapacity, loginRefillPerMinute), maxClientes, reloj);
        this.refreshBuckets = nuevaCache(inactividad(refreshCapacity, refreshRefillPerMinute), maxClientes, reloj);
        this.forgotBuckets = nuevaCache(inactividad(forgotCapacity, forgotRefillPerMinute), maxClientes, reloj);
        this.resetBuckets = nuevaCache(inactividad(resetCapacity, resetRefillPerMinute), maxClientes, reloj);
        this.changeBuckets = nuevaCache(inactividad(changeCapacity, changeRefillPerMinute), maxClientes, reloj);
        this.changeCapacity = changeCapacity;
        this.changeRefillPerMinute = changeRefillPerMinute;
        this.forgotCapacity = forgotCapacity;
        this.forgotRefillPerMinute = forgotRefillPerMinute;
        this.resetCapacity = resetCapacity;
        this.resetRefillPerMinute = resetRefillPerMinute;
        this.refreshCapacity = refreshCapacity;
        this.refreshRefillPerMinute = refreshRefillPerMinute;
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
        this.loginCapacity = loginCapacity;
        this.loginRefillPerMinute = loginRefillPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !API.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        String ip = resolveClientIp(request);
        Bucket bucket = buckets.get(ip, k -> newBucket(capacity, refillPerMinute));
        boolean permitido = bucket.tryConsume(1);

        if (permitido && LOGIN.matches(request)) {
            permitido = loginBuckets.get(ip, k -> newBucket(loginCapacity, loginRefillPerMinute))
                    .tryConsume(1);
        }
        if (permitido && REFRESH.matches(request)) {
            permitido = refreshBuckets.get(ip, k -> newBucket(refreshCapacity, refreshRefillPerMinute))
                    .tryConsume(1);
        }

        if (permitido && FORGOT.matches(request)) {
            permitido = forgotBuckets.get(ip, k -> newBucket(forgotCapacity, forgotRefillPerMinute))
                    .tryConsume(1);
        }
        // * Aceptar una invitación es el mismo paso que restablecer: comparten límite.
        if (permitido && (RESET.matches(request) || ACEPTAR.matches(request))) {
            permitido = resetBuckets.get(ip, k -> newBucket(resetCapacity, resetRefillPerMinute))
                    .tryConsume(1);
        }

        if (permitido && CAMBIAR.matches(request)) {
            permitido = changeBuckets.get(ip, k -> newBucket(changeCapacity, changeRefillPerMinute))
                    .tryConsume(1);
        }

        if (permitido) {
            filterChain.doFilter(request, response);
            return;
        }

        try (MDC.MDCCloseable ignored = MDC.putCloseable("module", "security")) {
            LOG.warn("Límite de peticiones alcanzado metodo={} ruta={} errorCode=ERR_SYS_02", request.getMethod(),
                    request.getRequestURI());
        }
        writeTooManyRequests(request, response);
    }

    /** Nunca menor que lo que tarda un bucket vacío en rellenarse (con margen). */
    static Duration inactividad(int capacidad, int recargaPorMinuto) {
        Duration rellenado = Duration.ofMinutes(2L * Math.ceilDiv(capacidad, Math.max(1, recargaPorMinuto)));
        return rellenado.compareTo(INACTIVIDAD_MINIMA) > 0 ? rellenado : INACTIVIDAD_MINIMA;
    }

    private static Cache<String, Bucket> nuevaCache(Duration inactividad, int maxClientes, Ticker reloj) {
        // * `executor` en el mismo hilo: sin tareas de fondo y con tests deterministas.
        return Caffeine.newBuilder()
                .expireAfterAccess(inactividad)
                .maximumSize(maxClientes)
                .ticker(reloj)
                .executor(Runnable::run)
                .build();
    }

    /** Entradas retenidas entre todas las cachés, tras aplicar expiración y máximo. */
    long entradasEnMemoria() {
        buckets.cleanUp();
        loginBuckets.cleanUp();
        refreshBuckets.cleanUp();
        forgotBuckets.cleanUp();
        resetBuckets.cleanUp();
        changeBuckets.cleanUp();
        return buckets.estimatedSize() + loginBuckets.estimatedSize() + refreshBuckets.estimatedSize()
                + forgotBuckets.estimatedSize() + resetBuckets.estimatedSize() + changeBuckets.estimatedSize();
    }

    private static RequestMatcher post(String ruta) {
        return RUTAS.matcher(HttpMethod.POST, "/api/v1/auth/" + ruta);
    }

    private static Bucket newBucket(int capacity, int refillPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // * request.getRemoteAddr() devuelve la IP REAL del cliente porque
    // * `server.forward-headers-strategy=native` (application.properties)
    // * hace que Tomcat la reemplace por la de X-Forwarded-For, pero SOLO si
    // * quien se conecta es un proxy interno de confianza. nginx sobrescribe
    // * ese header con $remote_addr (no lo acumula), así que un cliente no
    // * puede falsificarlo.
    // ! Si se agrega otro proxy/CDN delante, revisar la lista de proxies de
    // ! confianza (server.tomcat.remoteip.internal-proxies).
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        // * Se escribe el JSON a mano (no ApiResponse.error(...)) porque este
        // * filtro corre a nivel servlet, antes del dispatch a Spring MVC --
        // * el @RestControllerAdvice de GlobalExceptionHandler (ver
        // * EXCEPCIONES_BACKEND.md) nunca llega a intervenir acá. Igual se
        // * respeta el mismo contrato de sobre que RESPONSES_BACKEND.md exige
        // * en todo el resto de la API.
        String body = """
                {"status":"failed","data":null,"message":"%s","timestamp":"%s","error":"ERR_SYS_02"}"""
                .formatted(mensajes.en(mensajes.idiomaDe(request), "common.error.limitePeticiones"),
                        Instant.now().toString());
        response.getWriter().write(body);
    }
}
