package com.example.backend.features.usuarios.service;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.repository.UsuarioRepository;

/**
 * Consultas de usuarios para otros módulos.
 *
 * ! RLS decide qué se ve: el llamador tiene que haber fijado el contexto en su
 * ! propia transacción (por eso `MANDATORY`).
 */
@Service
@RequiredArgsConstructor
public class UsuarioConsultaService {

    private final UsuarioRepository usuarioRepository;

    /** Búsqueda por correo exacto (sin distinguir mayúsculas). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<UUID> codigoPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email.trim()).map(Usuario::getCodigo);
    }
}
