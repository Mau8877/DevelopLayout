package com.example.backend.features.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.backend.common.util.Sha256;
import com.example.backend.features.auth.enums.TipoCliente;
import com.example.backend.features.auth.model.Sesion;
import com.example.backend.features.auth.repository.SesionRepository;
import com.example.backend.features.usuarios.model.Usuario;

/**
 * Crea sesiones con un refresh token opaco.
 *
 * * El token en claro (256 bits) solo se devuelve al llamador; en la base
 * * queda únicamente su SHA-256.
 * ! Debe correr dentro de la transacción del login, con `app.current_user`
 * ! ya fijado (RLS exige que la fila sea del usuario del contexto).
 */
@Service
public class SesionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SesionRepository sesionRepository;
    private final Duration refreshTtl;

    public SesionService(SesionRepository sesionRepository,
            @Value("${app.auth.refresh-ttl:P7D}") Duration refreshTtl) {
        this.sesionRepository = sesionRepository;
        this.refreshTtl = refreshTtl;
    }

    public SesionCreada crear(Usuario usuario, TipoCliente tipo, String ip, String userAgent) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant ahora = Instant.now();
        Sesion sesion = new Sesion(usuario.getId(), usuario.getCodigo(), Sha256.hex(token), tipo,
                truncar(ip, 45), truncar(userAgent, 255), ahora.plus(refreshTtl));
        sesion.setUltimoUsoEn(ahora);
        return new SesionCreada(sesionRepository.save(sesion), token);
    }

    private static String truncar(String valor, int max) {
        return valor == null || valor.length() <= max ? valor : valor.substring(0, max);
    }

    /** El `refreshToken` es el valor en claro; no se loguea ni se persiste. */
    public record SesionCreada(Sesion sesion, String refreshToken) {
    }
}
