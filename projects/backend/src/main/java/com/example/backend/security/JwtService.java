package com.example.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.features.auth.AuthErrorCodes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida los access token JWT (HS256, vida corta).
 *
 * * El algoritmo queda fijo por la clave: JJWT rechaza `alg=none` y cualquier
 * * token firmado con otro algoritmo al verificar contra esta SecretKey.
 * ! El token no lleva datos personales: solo codigo, sesión y roles.
 */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration accessTtl;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl:PT15M}") Duration accessTtl,
            @Value("${app.jwt.issuer:app-backend}") String issuer) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES || secret.startsWith("CAMBIAME")) {
            throw new IllegalStateException(
                    "JWT_SECRET falta, es demasiado corta (mínimo 32 caracteres) o sigue con el valor de ejemplo.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = accessTtl;
        this.issuer = issuer;
    }

    public String emitir(UUID codigoUsuario, List<String> roles, UUID sesionCodigo) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(codigoUsuario.toString())
                .claim("sid", sesionCodigo.toString())
                .claim("roles", roles)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(accessTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public TokenInfo validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object rolesClaim = claims.get("roles");
            List<String> roles = rolesClaim instanceof List<?> lista
                    ? lista.stream().map(String::valueOf).toList()
                    : List.of();
            return new TokenInfo(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.get("sid", String.class)),
                    roles);
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_07, "common.error.noAutenticado");
        }
    }

    public record TokenInfo(UUID codigoUsuario, UUID sesionCodigo, List<String> roles) {
    }
}
