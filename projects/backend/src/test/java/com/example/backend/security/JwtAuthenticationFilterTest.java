package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.Cookie;

class JwtAuthenticationFilterTest {

    private final JwtService jwt = new JwtService("test-secret-test-secret-test-secret-1234",
            Duration.ofMinutes(15), "app-backend");
    private final SesionRevocadaRegistry revocadas = mock(SesionRevocadaRegistry.class);
    private final JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwt, revocadas);

    private final UUID usuario = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private final String token = jwt.emitir(usuario, List.of("USUARIO"), sesion);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Authentication ejecutar(MockHttpServletRequest request) throws Exception {
        SecurityContextHolder.clearContext();
        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void cookieValida_autenticaConRolesYSesion() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("accessToken", token));

        Authentication auth = ejecutar(req);

        assertThat(auth.getPrincipal()).isEqualTo(usuario);
        assertThat(auth.getDetails()).isEqualTo(sesion);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USUARIO");
    }

    @Test
    void bearerConHeaderMobile_autentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Client-Type", "mobile");
        req.addHeader("Authorization", "Bearer " + token);

        assertThat(ejecutar(req)).isNotNull();
    }

    @Test
    void cookieConHeaderMobile_noAutentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Client-Type", "mobile");
        req.setCookies(new Cookie("accessToken", token));

        assertThat(ejecutar(req)).isNull();
    }

    @Test
    void bearerSinHeaderMobile_noAutentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);

        assertThat(ejecutar(req)).isNull();
    }

    @Test
    void tokenManipulado_noAutentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("accessToken", token + "x"));

        assertThat(ejecutar(req)).isNull();
    }

    @Test
    void sesionRevocada_noAutentica() throws Exception {
        when(revocadas.estaRevocada(sesion)).thenReturn(true);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("accessToken", token));

        assertThat(ejecutar(req)).isNull();
    }

    @Test
    void siRedisFalla_noAutenticaYNoPropagaLaExcepcion() throws Exception {
        when(revocadas.estaRevocada(any())).thenThrow(new IllegalStateException("redis caído"));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("accessToken", token));

        assertThat(ejecutar(req)).isNull();
    }

    @Test
    void resolvers_devuelvenUsuarioYSesionDelContexto() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("accessToken", token));
        ejecutar(req);

        assertThat(new CurrentUserIdArgumentResolver().resolveArgument(mock(MethodParameter.class), null, null, null))
                .isEqualTo(usuario);
        assertThat(new CurrentSessionIdArgumentResolver().resolveArgument(mock(MethodParameter.class), null, null, null))
                .isEqualTo(sesion);
    }
}
