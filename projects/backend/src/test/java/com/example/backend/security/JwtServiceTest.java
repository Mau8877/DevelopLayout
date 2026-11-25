package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.backend.common.exception.UnauthorizedException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-1234";

    private JwtService servicio(String secret, Duration ttl) {
        return new JwtService(secret, ttl, "app-backend");
    }

    @Test
    void emitir_yValidar_devuelveElMismoCodigoSesionYRoles() {
        JwtService jwt = servicio(SECRET, Duration.ofMinutes(15));
        UUID usuario = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();

        JwtService.TokenInfo info = jwt.validar(jwt.emitir(usuario, List.of("USUARIO", "SUPER_ADMIN"), sesion));

        assertThat(info.codigoUsuario()).isEqualTo(usuario);
        assertThat(info.sesionCodigo()).isEqualTo(sesion);
        assertThat(info.roles()).containsExactly("USUARIO", "SUPER_ADMIN");
    }

    @Test
    void validar_conTokenVencido_lanzaUnauthorized() {
        JwtService jwt = servicio(SECRET, Duration.ofSeconds(-5));
        String token = jwt.emitir(UUID.randomUUID(), List.of("USUARIO"), UUID.randomUUID());

        assertThatThrownBy(() -> jwt.validar(token)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void validar_conFirmaDeOtraClave_lanzaUnauthorized() {
        String token = servicio("otra-clave-otra-clave-otra-clave-9999", Duration.ofMinutes(15))
                .emitir(UUID.randomUUID(), List.of("USUARIO"), UUID.randomUUID());

        assertThatThrownBy(() -> servicio(SECRET, Duration.ofMinutes(15)).validar(token))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void validar_conPayloadAlterado_lanzaUnauthorized() {
        JwtService jwt = servicio(SECRET, Duration.ofMinutes(15));
        String[] partes = jwt.emitir(UUID.randomUUID(), List.of("USUARIO"), UUID.randomUUID()).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("USUARIO", "SUPER_ADMIN");
        String alterado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        assertThatThrownBy(() -> jwt.validar(alterado)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void validar_conAlgNone_lanzaUnauthorized() {
        JwtService jwt = servicio(SECRET, Duration.ofMinutes(15));
        String[] partes = jwt.emitir(UUID.randomUUID(), List.of("USUARIO"), UUID.randomUUID()).split("\\.");
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> jwt.validar(header + "." + partes[1] + ".")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void validar_conBasura_lanzaUnauthorized() {
        JwtService jwt = servicio(SECRET, Duration.ofMinutes(15));

        assertThatThrownBy(() -> jwt.validar("no-es-un-jwt")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> jwt.validar("")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void constructor_conSecretCortaONula_falla() {
        assertThatThrownBy(() -> servicio("corta", Duration.ofMinutes(15))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> servicio(null, Duration.ofMinutes(15))).isInstanceOf(IllegalStateException.class);
    }
}
