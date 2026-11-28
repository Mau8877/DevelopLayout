package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void doFilterInternal_dentroDelLimite_dejaPasarTodasLasRequests() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(3, 60, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 100000);
        AtomicInteger vecesQueLlegoAlChain = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(
                    apiRequest("203.0.113.10"),
                    response,
                    (req, res) -> vecesQueLlegoAlChain.incrementAndGet());

            assertThat(response.getStatus()).isEqualTo(200);
        }

        assertThat(vecesQueLlegoAlChain.get()).isEqualTo(3);
    }

    @Test
    void doFilterInternal_superandoElLimite_devuelve429ConElSobreDeError() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 60, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 100000);
        AtomicInteger vecesQueLlegoAlChain = new AtomicInteger();

        // Agota el cupo (2 requests permitidas)
        for (int i = 0; i < 2; i++) {
            filter.doFilterInternal(
                    apiRequest("203.0.113.20"),
                    new MockHttpServletResponse(),
                    (req, res) -> vecesQueLlegoAlChain.incrementAndGet());
        }

        // La tercera ya no debería llegar al resto de la cadena
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                apiRequest("203.0.113.20"),
                response,
                (req, res) -> vecesQueLlegoAlChain.incrementAndGet());

        assertThat(vecesQueLlegoAlChain.get()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString())
                .contains("\"status\":\"failed\"")
                .contains("\"error\":\"ERR_SYS_02\"");
    }

    @Test
    void doFilterInternal_distintasIps_tienenCuposIndependientes() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 60, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 100000);
        AtomicInteger vecesQueLlegoAlChain = new AtomicInteger();

        filter.doFilterInternal(
                apiRequest("203.0.113.30"),
                new MockHttpServletResponse(),
                (req, res) -> vecesQueLlegoAlChain.incrementAndGet());

        // Otra IP: no debería verse afectada por el cupo agotado de la anterior
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                apiRequest("203.0.113.31"),
                response,
                (req, res) -> vecesQueLlegoAlChain.incrementAndGet());

        assertThat(vecesQueLlegoAlChain.get()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_loginConLimitePropio_rechazaAntesQueElGlobal() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(100, 100, 2, 60, 1000, 1000, 1000, 1000, 1000, 1000, 100000);
        AtomicInteger llegaron = new AtomicInteger();
        MockHttpServletResponse ultima = new MockHttpServletResponse();

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest login = requestTo("/api/v1/auth/login");
            login.setMethod("POST");
            login.setRemoteAddr("203.0.113.40");
            ultima = new MockHttpServletResponse();
            filter.doFilterInternal(login, ultima, (req, res) -> llegaron.incrementAndGet());
        }

        assertThat(llegaron.get()).isEqualTo(2);
        assertThat(ultima.getStatus()).isEqualTo(429);

        // Otro endpoint de la misma IP sigue dentro del límite global.
        MockHttpServletRequest otro = apiRequest("203.0.113.40");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filter.doFilterInternal(otro, respuesta, (req, res) -> llegaron.incrementAndGet());
        assertThat(respuesta.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_refreshConLimitePropio_rechazaAlSuperarlo() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(100, 100, 100, 100, 2, 60, 1000, 1000, 1000, 1000, 100000);
        AtomicInteger llegaron = new AtomicInteger();
        MockHttpServletResponse ultima = new MockHttpServletResponse();

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest refresh = requestTo("/api/v1/auth/refresh");
            refresh.setMethod("POST");
            refresh.setRemoteAddr("203.0.113.50");
            ultima = new MockHttpServletResponse();
            filter.doFilterInternal(refresh, ultima, (req, res) -> llegaron.incrementAndGet());
        }

        assertThat(llegaron.get()).isEqualTo(2);
        assertThat(ultima.getStatus()).isEqualTo(429);
    }

    @Test
    void entradasSinUso_seLiberan() throws Exception {
        AtomicLong nanos = new AtomicLong();
        RateLimitFilter filter = new RateLimitFilter(100, 100, 10, 10, 30, 30, 1000, 1000, 1000, 1000, 100000, nanos::get);
        for (int i = 0; i < 500; i++) {
            MockHttpServletRequest login = requestTo("/api/v1/auth/login");
            login.setMethod("POST");
            login.setRemoteAddr("2001:db8::" + Integer.toHexString(i));
            filter.doFilterInternal(login, new MockHttpServletResponse(), (req, res) -> {
            });
        }
        assertThat(filter.entradasEnMemoria()).isEqualTo(1000);

        nanos.addAndGet(Duration.ofMinutes(9).toNanos());
        assertThat(filter.entradasEnMemoria()).isEqualTo(1000);
        nanos.addAndGet(Duration.ofMinutes(2).toNanos());

        assertThat(filter.entradasEnMemoria()).isZero();
    }

    @Test
    void conMasClientesQueElMaximo_laMemoriaTieneTecho() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(100, 100, 10, 10, 30, 30, 1000, 1000, 1000, 1000, 200);
        for (int i = 0; i < 5000; i++) {
            filter.doFilterInternal(apiRequest("2001:db8::" + Integer.toHexString(i)),
                    new MockHttpServletResponse(), (req, res) -> {
                    });
        }

        assertThat(filter.entradasEnMemoria()).isLessThanOrEqualTo(200);
    }

    @Test
    void inactividad_nuncaEsMenorQueElTiempoDeRellenado() {
        assertThat(RateLimitFilter.inactividad(100, 100)).isEqualTo(Duration.ofMinutes(10));
        assertThat(RateLimitFilter.inactividad(100, 1)).isEqualTo(Duration.ofMinutes(200));
    }

    @Test
    void shouldNotFilter_rutasFueraDeApiV1_quedanExcluidas() {
        RateLimitFilter filter = new RateLimitFilter(100, 100, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 100000);

        assertThat(filter.shouldNotFilter(requestTo("/health"))).isTrue();
        assertThat(filter.shouldNotFilter(requestTo("/prometheus"))).isTrue();
        assertThat(filter.shouldNotFilter(requestTo("/swagger-ui/index.html"))).isTrue();
        assertThat(filter.shouldNotFilter(requestTo("/api/v1/users"))).isFalse();
    }

    private static MockHttpServletRequest apiRequest(String remoteAddr) {
        MockHttpServletRequest request = requestTo("/api/v1/ping");
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static MockHttpServletRequest requestTo(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    @Test
    void recuperacion_cadaRutaTieneSuLimitePorIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(100, 100, 100, 100, 100, 100, 2, 1, 3, 1, 100000);

        assertThat(estadosDePost(filter, "/api/v1/auth/password/forgot", 3)).containsExactly(200, 200, 429);
        assertThat(estadosDePost(filter, "/api/v1/auth/password/reset", 4)).containsExactly(200, 200, 200, 429);
        assertThat(estadosDePost(filter, "/api/v1/auth/login", 1)).containsExactly(200);
    }

    @Test
    void aceptarInvitacion_comparteElLimitePorIpDelRestablecimiento() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(100, 100, 100, 100, 100, 100, 100, 100, 3, 1, 100000);

        assertThat(estadosDePost(filter, "/api/v1/auth/invitation/accept", 2)).containsExactly(200, 200);
        assertThat(estadosDePost(filter, "/api/v1/auth/password/reset", 2)).containsExactly(200, 429);
        assertThat(estadosDePost(filter, "/api/v1/auth/invitation/accept", 1)).containsExactly(429);
    }

    @Test
    void elMensajeDel429SaleEnElIdiomaPedido() throws Exception {
        for (String[] caso : new String[][] {
            {"en", "Too many requests. Try again in a few seconds"},
            {null, "Demasiadas solicitudes. Inténtalo de nuevo en unos segundos"},
        }) {
            RateLimitFilter filter = new RateLimitFilter(1, 60, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 100000);
            MockHttpServletResponse ultima = new MockHttpServletResponse();
            for (int i = 0; i < 2; i++) {
                MockHttpServletRequest request = apiRequest("203.0.113.99");
                if (caso[0] != null) {
                    request.addHeader("Accept-Language", caso[0]);
                    request.setPreferredLocales(java.util.List.of(java.util.Locale.forLanguageTag(caso[0])));
                }
                ultima = new MockHttpServletResponse();
                ultima.setCharacterEncoding("UTF-8");
                filter.doFilterInternal(request, ultima, (req, res) -> { });
            }
            assertThat(ultima.getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                    .contains(caso[1], "ERR_SYS_02");
        }
    }

    private static java.util.List<Integer> estadosDePost(RateLimitFilter filter, String ruta, int veces)
            throws Exception {
        java.util.List<Integer> estados = new java.util.ArrayList<>();
        for (int i = 0; i < veces; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", ruta);
            request.setRemoteAddr("203.0.113.90");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, (req, res) -> { });
            estados.add(response.getStatus());
        }
        return estados;
    }
}
