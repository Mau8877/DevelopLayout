package com.example.backend.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limit por IP sobre /api/v1/** (endpoints de negocio).
 *
 * * En memoria (ConcurrentHashMap de buckets), no distribuido -- correcto
 * * para una sola instancia de backend (el caso de este proyecto hoy). Si
 * * en algún momento se corre más de una réplica del backend, cada una
 * * tendría su propio límite independiente en vez de uno compartido; para
 * * eso hay que migrar a bucket4j_jdk11-redis (Redis ya está en el stack
 * * y spring-boot-starter-data-redis ya está en el pom.xml).
 * ! No aplica a /health, /prometheus ni /swagger-ui/** -- ver shouldNotFilter.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // * Límite más estricto, por IP, solo para el login (además del global).
    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    // * /refresh no exige access token: lleva su propio límite por IP.
    private final ConcurrentHashMap<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

    private final int capacity;
    private final int refillPerMinute;
    private final int loginCapacity;
    private final int loginRefillPerMinute;
    private final int refreshCapacity;
    private final int refreshRefillPerMinute;

    public RateLimitFilter(
            @Value("${app.ratelimit.capacity}") int capacity,
            @Value("${app.ratelimit.refill-per-minute}") int refillPerMinute,
            @Value("${app.ratelimit.login-capacity:10}") int loginCapacity,
            @Value("${app.ratelimit.login-refill-per-minute:10}") int loginRefillPerMinute,
            @Value("${app.ratelimit.refresh-capacity:30}") int refreshCapacity,
            @Value("${app.ratelimit.refresh-refill-per-minute:30}") int refreshRefillPerMinute) {
        this.refreshCapacity = refreshCapacity;
        this.refreshRefillPerMinute = refreshRefillPerMinute;
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
        this.loginCapacity = loginCapacity;
        this.loginRefillPerMinute = loginRefillPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket(capacity, refillPerMinute));
        boolean permitido = bucket.tryConsume(1);

        if (permitido && esPost(request, "/api/v1/auth/login")) {
            permitido = loginBuckets.computeIfAbsent(ip, k -> newBucket(loginCapacity, loginRefillPerMinute))
                    .tryConsume(1);
        }
        if (permitido && esPost(request, "/api/v1/auth/refresh")) {
            permitido = refreshBuckets.computeIfAbsent(ip, k -> newBucket(refreshCapacity, refreshRefillPerMinute))
                    .tryConsume(1);
        }

        if (permitido) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response);
    }

    private static boolean esPost(HttpServletRequest request, String uri) {
        return "POST".equalsIgnoreCase(request.getMethod()) && uri.equals(request.getRequestURI());
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

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        // * Se escribe el JSON a mano (no ApiResponse.error(...)) porque este
        // * filtro corre a nivel servlet, antes del dispatch a Spring MVC --
        // * el @RestControllerAdvice de GlobalExceptionHandler (ver
        // * EXCEPCIONES_BACKEND.md) nunca llega a intervenir acá. Igual se
        // * respeta el mismo contrato de sobre que RESPONSES_BACKEND.md exige
        // * en todo el resto de la API.
        String body = """
                {"status":"failed","data":null,"message":"Demasiadas solicitudes, intentá de nuevo en unos segundos","timestamp":"%s","error":"ERR_SYS_02"}"""
                .formatted(Instant.now().toString());
        response.getWriter().write(body);
    }
}
