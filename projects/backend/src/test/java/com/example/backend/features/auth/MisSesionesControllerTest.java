package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Mis sesiones (HU-INICIAL-Sesiones). La app corre como app_runtime: RLS aplica de verdad. */
class MisSesionesControllerTest extends AuthSesionIntegrationTest {

    private static final String RUTA = "/api/v1/auth/sessions";

    // ---------------------------------------------------------------- apoyo

    private static MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder request, Tokens tokens) {
        // * Como la app: el Bearer solo se acepta junto con la cabecera de cliente mobile.
        return request.header("X-Client-Type", "mobile").header("Authorization", "Bearer " + tokens.access());
    }

    private MvcResult listar(Tokens tokens, String query) throws Exception {
        return mvc.perform(con(get(RUTA + query), tokens)).andReturn();
    }

    private MvcResult cerrar(Tokens tokens, String codigo) throws Exception {
        return mvc.perform(con(delete(RUTA + "/" + codigo), tokens)).andReturn();
    }

    private MvcResult cerrarOtras(Tokens tokens) throws Exception {
        return mvc.perform(con(post(RUTA + "/revoke-others"), tokens)).andReturn();
    }

    private static int estado(MvcResult res) {
        return res.getResponse().getStatus();
    }

    private static <T> T leer(MvcResult res, String ruta) throws Exception {
        return JsonPath.read(res.getResponse().getContentAsString(), ruta);
    }

    private static String error(MvcResult res) throws Exception {
        assertThat((String) leer(res, "$.status")).isEqualTo("failed");
        return leer(res, "$.error");
    }

    /** Código de la sesión de estos tokens, tal como la API la marca como actual. */
    private String codigoDe(Tokens tokens) throws Exception {
        List<String> actuales = leer(listar(tokens, "?pageSize=100"), "$.data[?(@.actual == true)].codigo");
        assertThat(actuales).hasSize(1);
        return actuales.getFirst();
    }

    private int activas() {
        return dueno().queryForObject("SELECT count(*) FROM authentication.sesion s JOIN authentication.usuario u "
                + "ON u.id = s.usuario_id WHERE u.email = ? AND s.estado = 'ACTIVA'", Integer.class, email);
    }

    private String sesion(String codigo, String columna) {
        return dueno().queryForObject("SELECT " + columna + "::text FROM authentication.sesion "
                + "WHERE codigo_sesion = ?::uuid", String.class, codigo);
    }

    private void actualizar(String codigo, String asignacion) {
        dueno().update("UPDATE authentication.sesion SET " + asignacion + " WHERE codigo_sesion = ?::uuid", codigo);
    }

    /** Otro usuario con una sesión abierta; deja `email` como estaba. */
    private Tokens loginDeOtroUsuario(String rol) throws Exception {
        String propio = email;
        prepararUsuario();
        if (!"USUARIO".equals(rol)) {
            dueno().update("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id) "
                    + "SELECT gen_random_uuid(), u.id, r.id FROM authentication.usuario u, authentication.rol r "
                    + "WHERE u.email = ? AND r.nombre = ?", email, rol);
        }
        Tokens tokens = loginMobile();
        email = propio;
        return tokens;
    }

    // ---------------------------------------------------------------- listar

    @Test
    void listar_devuelveSoloLasPropiasVigentesConLaActualPrimeroYSinDatosInternos() throws Exception {
        Tokens actual = loginMobile();
        Tokens otra = loginWeb();
        Tokens vencida = loginMobile();
        actualizar(codigoDe(vencida), "expira_en = now() - interval '1 minute'");
        loginDeOtroUsuario("USUARIO");

        MvcResult res = listar(actual, "");

        assertThat(estado(res)).isEqualTo(200);
        assertThat((List<?>) leer(res, "$.data")).hasSize(2);
        assertThat((Boolean) leer(res, "$.data[0].actual")).isTrue();
        assertThat((Boolean) leer(res, "$.data[1].actual")).isFalse();
        assertThat((String) leer(res, "$.data[1].codigo")).isEqualTo(codigoDe(otra));
        assertThat((Integer) leer(res, "$.meta.totalItems")).isEqualTo(2);
        assertThat((Integer) leer(res, "$.meta.page")).isEqualTo(1);
        assertThat((Integer) leer(res, "$.meta.pageSize")).isEqualTo(20);
        assertThat(res.getResponse().getContentAsString()).doesNotContainIgnoringCase("hash")
                .doesNotContain("usuarioCodigo", "usuarioId", "\"id\"");
    }

    @Test
    void listar_conPaginaDeUno_laActualVieneEnLaPrimeraAunqueSeaLaMasVieja() throws Exception {
        Tokens vieja = loginMobile();
        loginMobile();
        loginMobile();

        MvcResult primera = listar(vieja, "?pageSize=1");

        assertThat((Boolean) leer(primera, "$.data[0].actual")).isTrue();
        assertThat((Integer) leer(primera, "$.meta.totalPages")).isEqualTo(3);
        assertThat((Boolean) leer(listar(vieja, "?pageSize=1&page=2"), "$.data[0].actual")).isFalse();
        assertThat(estado(listar(vieja, "?pageSize=101"))).isEqualTo(400);
    }

    // ---------------------------------------------------------------- autorización

    @Test
    void sinSesion_da401_yEnWebSinCsrfDa403() throws Exception {
        Tokens web = loginWeb();
        String otra = codigoDe(loginMobile());

        assertThat(estado(mvc.perform(get(RUTA)).andReturn())).isEqualTo(401);
        assertThat(estado(mvc.perform(delete(RUTA + "/" + otra).with(csrf())).andReturn())).isEqualTo(401);
        assertThat(estado(mvc.perform(delete(RUTA + "/" + otra).cookie(new Cookie("accessToken", web.access())))
                .andReturn())).isEqualTo(403);
        assertThat(estado(mvc.perform(post(RUTA + "/revoke-others").cookie(new Cookie("accessToken", web.access())))
                .andReturn())).isEqualTo(403);
        assertThat(activas()).isEqualTo(2);
        assertThat(estado(mvc.perform(delete(RUTA + "/" + otra).with(csrf())
                .cookie(new Cookie("accessToken", web.access()))).andReturn())).isEqualTo(200);
    }

    @Test
    void sesionAjenaOInexistente_daElMismo404_tambienParaUnAdministrador() throws Exception {
        Tokens mia = loginMobile();
        String miCodigo = codigoDe(mia);
        Tokens usuario = loginDeOtroUsuario("USUARIO");
        Tokens administrador = loginDeOtroUsuario("SUPER_ADMIN");

        for (Tokens intruso : List.of(usuario, administrador)) {
            MvcResult ajena = cerrar(intruso, miCodigo);
            MvcResult inexistente = cerrar(intruso, UUID.randomUUID().toString());
            assertThat(estado(ajena)).isEqualTo(404);
            assertThat(estado(inexistente)).isEqualTo(404);
            assertThat(error(ajena)).isEqualTo(error(inexistente)).isEqualTo("ERR_AUTH_09");
            assertThat((List<String>) leer(listar(intruso, "?pageSize=100"), "$.data[*].codigo"))
                    .doesNotContain(miCodigo).hasSize(1);
            assertThat(estado(cerrarOtras(intruso))).isEqualTo(200);
        }

        assertThat(sesion(miCodigo, "estado")).isEqualTo("ACTIVA");
        assertAccess(mia.access(), 200);
    }

    // ---------------------------------------------------------------- cerrar

    @Test
    void cerrarOtra_dejaDeServirAlInstante_laActualSigue_yQuedaEnLaBitacora() throws Exception {
        Tokens actual = loginMobile();
        Tokens otra = loginMobile();
        String codigo = codigoDe(otra);

        MvcResult res = cerrar(actual, codigo);

        assertThat(estado(res)).isEqualTo(200);
        assertThat((String) leer(res, "$.status")).isEqualTo("success");
        assertThat(sesion(codigo, "estado")).isEqualTo("REVOCADA");
        assertThat(sesion(codigo, "motivo_revocacion")).isEqualTo("USUARIO");
        assertAccess(otra.access(), 401);
        assertThat(estado(mvc.perform(mobile("refresh", otra.refresh())).andReturn())).isEqualTo(401);
        assertAccess(actual.access(), 200);
        assertThat(eventos("AUTH_SESION_CERRADA")).isEqualTo(1);
        assertThat(dueno().queryForObject("SELECT b.detalle::text FROM authentication.bitacora b "
                + "WHERE b.entidad_codigo = ?::uuid AND b.accion = 'AUTH_SESION_CERRADA'", String.class, codigo))
                .contains("USUARIO").doesNotContain(email);
    }

    @Test
    void cerrarLaActual_da400_yNoCierraNada() throws Exception {
        Tokens actual = loginMobile();

        MvcResult res = cerrar(actual, codigoDe(actual));

        assertThat(estado(res)).isEqualTo(400);
        assertThat(error(res)).isEqualTo("ERR_AUTH_10");
        assertAccess(actual.access(), 200);
        assertThat(eventos("AUTH_SESION_CERRADA")).isZero();
    }

    @Test
    void cerrarDosVecesLaMisma_da200_ySoloRegistraUnEvento() throws Exception {
        Tokens actual = loginMobile();
        String otra = codigoDe(loginMobile());

        assertThat(estado(cerrar(actual, otra))).isEqualTo(200);
        assertThat(estado(cerrar(actual, otra))).isEqualTo(200);

        assertThat(eventos("AUTH_SESION_CERRADA")).isEqualTo(1);
    }

    @Test
    void cerrarOtras_alcanzaTambienUnaVencida_yDiceCuantas() throws Exception {
        Tokens actual = loginMobile();
        Tokens otra = loginMobile();
        Tokens vencida = loginWeb();
        String codigoVencida = codigoDe(vencida);
        actualizar(codigoVencida, "expira_en = now() - interval '1 minute'");

        MvcResult res = cerrarOtras(actual);

        assertThat(estado(res)).isEqualTo(200);
        assertThat((String) leer(res, "$.message")).endsWith(": 2");
        assertThat(sesion(codigoVencida, "estado")).isEqualTo("REVOCADA");
        assertAccess(otra.access(), 401);
        assertAccess(vencida.access(), 401);
        assertAccess(actual.access(), 200);
        assertThat(activas()).isEqualTo(1);
        assertThat(eventos("AUTH_SESION_CERRADA")).isEqualTo(2);
    }

    // ---------------------------------------------------------------- carreras

    // * Determinista: la sesión que pide ya está revocada en la base pero su token
    // * todavía pasa el filtro (como en una petición que llegó justo antes).
    @Test
    void siLaSesionQuePideYaNoEstaActiva_da401_yNoCierraNada() throws Exception {
        Tokens revocada = loginMobile();
        String propia = codigoDe(revocada);
        String legitima = codigoDe(loginMobile());
        actualizar(propia, "estado = 'REVOCADA', motivo_revocacion = 'USUARIO', revocada_en = now()");

        MvcResult una = cerrar(revocada, legitima);
        MvcResult otras = cerrarOtras(revocada);

        assertThat(estado(una)).isEqualTo(401);
        assertThat(estado(otras)).isEqualTo(401);
        assertThat(error(una)).isEqualTo("ERR_AUTH_04");
        assertThat(sesion(legitima, "estado")).isEqualTo("ACTIVA");
        assertThat(eventos("AUTH_SESION_CERRADA")).isZero();
    }

    @Test
    void dosSesionesQueSeCierranUnaAOtra_dejanExactamenteUnaActiva() throws Exception {
        for (int vuelta = 0; vuelta < 8; vuelta++) {
            dueno().update("DELETE FROM authentication.sesion s USING authentication.usuario u "
                    + "WHERE u.id = s.usuario_id AND u.email = ?", email);
            Tokens a = loginMobile();
            Tokens b = loginMobile();
            String codigoA = codigoDe(a);
            String codigoB = codigoDe(b);
            boolean todas = vuelta % 2 == 1;
            List<Callable<Integer>> cruzados = new ArrayList<>();
            cruzados.add(() -> estado(todas ? cerrarOtras(a) : cerrar(a, codigoB)));
            cruzados.add(() -> estado(todas ? cerrarOtras(b) : cerrar(b, codigoA)));

            List<Integer> estados = new ArrayList<>();
            try (var hilos = Executors.newFixedThreadPool(2)) {
                for (var futuro : hilos.invokeAll(cruzados)) {
                    estados.add(futuro.get());
                }
            }

            assertThat(estados).as("vuelta %d", vuelta).containsExactlyInAnyOrder(200, 401);
            assertThat(activas()).as("vuelta %d", vuelta).isEqualTo(1);
        }
    }
}
