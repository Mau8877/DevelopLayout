package com.example.backend.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.features.usuarios.repository.UsuarioRolRepository;

/**
 * Carga un usuario por correo con RLS activo y arma sus authorities.
 *
 * * Primero fija `app.login_email` (para poder ver su fila) y, una vez
 * * encontrado, `app.current_user` (para ver sus roles).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RlsContext rlsContext;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository,
            UsuarioRolRepository usuarioRolRepository, RlsContext rlsContext) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.rlsContext = rlsContext;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        String normalizado = email == null ? "" : email.trim();
        rlsContext.usarCorreoDeLogin(normalizado);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        rlsContext.usarUsuario(usuario.getCodigo());

        List<GrantedAuthority> authorities = usuarioRolRepository.findActivosByUsuario(usuario).stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol.getNombre()))
                .toList();

        return User.withUsername(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .disabled(usuario.getEstado() != EstadoUsuario.ACTIVO)
                .build();
    }
}
