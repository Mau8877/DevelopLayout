package com.example.backend.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee el access token y puebla el SecurityContext.
 *
 * * Canal único por request: con `X-Client-Type: mobile` solo mira el header
 * * Bearer; sin él, solo la cookie `accessToken`. Nunca se mezclan.
 * * Falla cerrado: token inválido, revocado o error de Redis => no autentica
 * * (el entry point responde 401).
 * ! NO es un @Component: Spring Boot registraría además una copia en la
 * ! cadena de servlets, fuera de Spring Security, y esa copia dejaría
 * ! marcada la request como ya filtrada. Se crea en SecurityConfig.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_ACCESS = "accessToken";

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final SesionRevocadaRegistry revocadas;

    public JwtAuthenticationFilter(JwtService jwtService, SesionRevocadaRegistry revocadas) {
        this.jwtService = jwtService;
        this.revocadas = revocadas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            extraerToken(request).ifPresent(this::autenticar);
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            LOG.warn("No se pudo autenticar el access token: {}", e.getClass().getSimpleName());
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String token) {
        JwtService.TokenInfo info = jwtService.validar(token);
        if (revocadas.estaRevocada(info.sesionCodigo())) {
            return;
        }
        List<GrantedAuthority> authorities = info.roles().stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(info.codigoUsuario(), null, authorities);
        auth.setDetails(info.sesionCodigo());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Optional<String> extraerToken(HttpServletRequest request) {
        if ("mobile".equalsIgnoreCase(request.getHeader("X-Client-Type"))) {
            String header = request.getHeader("Authorization");
            return header != null && header.startsWith("Bearer ")
                    ? Optional.of(header.substring(7).trim()) : Optional.empty();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie c : cookies) {
            if (COOKIE_ACCESS.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }
}
