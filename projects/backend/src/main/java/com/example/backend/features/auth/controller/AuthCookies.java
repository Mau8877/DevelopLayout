package com.example.backend.features.auth.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Crea y borra las cookies de sesión del cliente web.
 *
 * * HttpOnly + SameSite=Strict; `Secure` según app.cookies.secure (false solo
 * * en dev, que corre sobre HTTP).
 * * accessToken: Path /api (15 min). refreshToken: Path /api/v1/auth (7 días),
 * * así el refresh token solo viaja a los endpoints de auth.
 */
@Component
public class AuthCookies {

    public static final String ACCESS = "accessToken";
    public static final String REFRESH = "refreshToken";

    private static final String PATH_ACCESS = "/api";
    private static final String PATH_REFRESH = "/api/v1/auth";

    private final boolean secure;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public AuthCookies(@Value("${app.cookies.secure:true}") boolean secure,
            @Value("${app.jwt.access-ttl:PT15M}") Duration accessTtl,
            @Value("${app.auth.refresh-ttl:P7D}") Duration refreshTtl) {
        this.secure = secure;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public void agregarTokens(HttpHeaders headers, String accessToken, String refreshToken) {
        headers.add(HttpHeaders.SET_COOKIE, construir(ACCESS, accessToken, PATH_ACCESS, accessTtl).toString());
        headers.add(HttpHeaders.SET_COOKIE, construir(REFRESH, refreshToken, PATH_REFRESH, refreshTtl).toString());
    }

    public void agregarBorrado(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, construir(ACCESS, "", PATH_ACCESS, Duration.ZERO).toString());
        headers.add(HttpHeaders.SET_COOKIE, construir(REFRESH, "", PATH_REFRESH, Duration.ZERO).toString());
    }

    private ResponseCookie construir(String nombre, String valor, String path, Duration maxAge) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
