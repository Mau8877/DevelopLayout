package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.backend.config.RecuperacionEjecutorConfig;
import com.example.backend.features.auth.service.PasswordResetTokenService;
import com.example.backend.features.auth.service.RecuperacionService;
import com.jayway.jsonpath.JsonPath;

/** Recuperación de contraseña de punta a punta (HU-INICIAL-OlvidePassword). */
@ExtendWith(OutputCaptureExtension.class)
class AuthPasswordResetTest extends AuthSesionIntegrationTest {

    private static final String NUEVA = "OtraClaveNueva456"; // gitleaks:allow
    private static final Pattern ENLACE =
            Pattern.compile("http://localhost:5173/reset-password#token=([A-Za-z0-9_-]+)");

    @MockitoBean
    private JavaMailSender mail;

    @MockitoSpyBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenService tokens;

    @Autowired
    private RecuperacionService recuperacion;

    @Autowired
    @Qualifier(RecuperacionEjecutorConfig.EJECUTOR)
    private ThreadPoolTaskExecutor ejecutor;

    private final List<SimpleMailMessage> bandeja = new CopyOnWriteArrayList<>();

    @BeforeEach
    void capturarCorreos() {
        bandeja.clear();
        doAnswer(llamada -> bandeja.add(llamada.getArgument(0))).when(mail).send(any(SimpleMailMessage.class));
    }

    // ---------------------------------------------------------------- apoyo

    private static String cuerpoForgot(String correo) {
        return "{\"email\":\"" + correo + "\"}";
    }

    private static String cuerpoReset(String token, String password) {
        return "{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}";
    }

    private static MockHttpServletRequestBuilder publico(String ruta, String cuerpo) {
        return post("/api/v1/auth/" + ruta).with(csrf()).header("User-Agent", "navegador-de-prueba")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private MvcResult olvide(String correo) throws Exception {
        return mvc.perform(publico("password/forgot", cuerpoForgot(correo))).andReturn();
    }

    private MvcResult restablecer(String token, String password) throws Exception {
        return mvc.perform(publico("password/reset", cuerpoReset(token, password))).andReturn();
    }

    private static int estado(MvcResult res) {
        return res.getResponse().getStatus();
    }

    private static String error(MvcResult res) throws Exception {
        assertThat((String) JsonPath.read(res.getResponse().getContentAsString(), "$.status")).isEqualTo("failed");
        return JsonPath.read(res.getResponse().getContentAsString(), "$.error");
    }

    private int login(String password, String ip) throws Exception {
        return estado(mvc.perform(post("/api/v1/auth/login").header("X-Client-Type", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                })).andReturn());
    }

    private void esperarEjecutorLibre() {
        await().atMost(Duration.ofSeconds(15)).until(() -> ejecutor.getThreadPoolExecutor().getQueue().isEmpty()
                && ejecutor.getActiveCount() == 0);
    }

    private List<SimpleMailMessage> correosA(String destino) {
        return bandeja.stream().filter(c -> c.getTo() != null && c.getTo()[0].equals(destino)).toList();
    }

    /** Pide un enlace por la API y devuelve el token del correo. */
    private String pedirToken() throws Exception {
        int antes = correosA(email).size();
        assertThat(estado(olvide(email))).isEqualTo(200);
        await().atMost(Duration.ofSeconds(15)).until(() -> correosA(email).size() == antes + 1);
        esperarEjecutorLibre();
        return tokenDe(correosA(email).getLast());
    }

    private static String tokenDe(SimpleMailMessage correo) {
        Matcher m = ENLACE.matcher(correo.getText());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private UUID codigoUsuario() {
        return UUID.fromString(dueno().queryForObject(
                "SELECT codigo_usuario::text FROM authentication.usuario WHERE email = ?", String.class, email));
    }

    private int sesionesActivas() {
        return dueno().queryForObject("SELECT count(*) FROM authentication.sesion s JOIN authentication.usuario u "
                + "ON u.id = s.usuario_id WHERE u.email = ? AND s.estado = 'ACTIVA'", Integer.class, email);
    }

    private String bitacora(String accion, String columna) {
        return dueno().queryForObject("SELECT string_agg(coalesce(b." + columna + "::text, ''), '|') "
                + "FROM authentication.bitacora b JOIN authentication.usuario u ON u.codigo_usuario = b.usuario_codigo "
                + "WHERE u.email = ? AND b.accion = ?", String.class, email, accion);
    }

    // ---------------------------------------------------------------- solicitar

    @Test
    void olvide_conCuentaActiva_enviaElEnlaceYLoRegistra(CapturedOutput salida) throws Exception {
        String token = pedirToken();

        assertThat(tokens.consultar(token)).contains(codigoUsuario());
        assertThat(bitacora("AUTH_RECUPERACION_SOLICITADA", "resultado")).isEqualTo("EXITO");
        assertThat(bitacora("AUTH_RECUPERACION_SOLICITADA", "ip")).isEqualTo("127.0.0.1");
        assertThat(bitacora("AUTH_RECUPERACION_SOLICITADA", "user_agent")).isEqualTo("navegador-de-prueba");
        assertThat(bitacora("AUTH_RECUPERACION_SOLICITADA", "detalle")).doesNotContain(token).doesNotContain(email);
        assertThat(salida.getOut()).doesNotContain(token);
    }

    @Test
    void olvide_respondeIgualExistaONoLaCuenta_ySoloEnviaALaActiva() throws Exception {
        String inactiva = "inactiva-" + email;
        dueno().update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                + "VALUES (gen_random_uuid(), ?, 'x', 'INACTIVO')", inactiva);

        List<String> respuestas = new ArrayList<>();
        for (String correo : List.of(email, "nadie-" + email, inactiva)) {
            MvcResult res = olvide(correo);
            assertThat(estado(res)).isEqualTo(200);
            respuestas.add(res.getResponse().getContentAsString().replaceAll("\"timestamp\":\"[^\"]+\"", ""));
        }
        await().atMost(Duration.ofSeconds(15)).until(() -> !correosA(email).isEmpty());
        esperarEjecutorLibre();

        assertThat(respuestas).containsOnly(respuestas.getFirst());
        assertThat(respuestas.getFirst()).contains("\"status\":\"success\"").doesNotContain("\"data\":{");
        assertThat(bandeja).hasSize(1);
    }

    // * Sin cronómetro: con los hilos del ejecutor ocupados, nada de lo que depende
    // * de la cuenta puede haber ocurrido, y la respuesta tiene que volver igual.
    @Test
    void olvide_respondeSinHacerNadaQueDependaDeLaCuenta() throws Exception {
        CountDownLatch soltar = new CountDownLatch(1);
        for (int i = 0; i < ejecutor.getCorePoolSize(); i++) {
            ejecutor.execute(() -> {
                try {
                    soltar.await(20, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        try {
            assertThat(estado(olvide(email))).isEqualTo(200);
            assertThat(bandeja).isEmpty();
            assertThat(eventos("AUTH_RECUPERACION_SOLICITADA")).isZero();
        } finally {
            soltar.countDown();
        }
        await().atMost(Duration.ofSeconds(15)).until(() -> correosA(email).size() == 1);
    }

    @Test
    void olvide_conElCorreoCaido_respondeIgualSigueSanaYLoRegistraSinDatos(CapturedOutput salida) throws Exception {
        doAnswer(llamada -> {
            throw new MailSendException("rechazado para " + email);
        }).when(mail).send(any(SimpleMailMessage.class));

        assertThat(estado(olvide(email))).isEqualTo(200);
        await().atMost(Duration.ofSeconds(15))
                .until(() -> eventos("AUTH_RECUPERACION_SOLICITADA") == 1);

        assertThat(bitacora("AUTH_RECUPERACION_SOLICITADA", "resultado")).isEqualTo("FALLO");
        assertThat(salida.getOut()).contains("No se pudo enviar el enlace").doesNotContain("rechazado para");
        assertThat(estado(mvc.perform(get("/health")).andReturn())).isEqualTo(200);
    }

    @Test
    void olvide_cuartaVezEnUnaHora_respondeIgualNoEnviaYElUltimoEnlaceSirve() throws Exception {
        pedirToken();
        pedirToken();
        String ultimo = pedirToken();

        assertThat(estado(olvide(email.toUpperCase()))).isEqualTo(200);
        esperarEjecutorLibre();

        assertThat(correosA(email)).hasSize(3);
        assertThat(estado(restablecer(ultimo, NUEVA))).isEqualTo(200);
    }

    @Test
    void endpointsPublicos_enWebExigenCsrf_yValidanElBody() throws Exception {
        MvcResult sinCsrf = mvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoForgot(email))).andReturn();
        MvcResult correoInvalido = olvide("no-es-un-correo");
        MvcResult campoDeMas = mvc.perform(publico("password/reset",
                "{\"token\":\"x\",\"password\":\"" + NUEVA + "\",\"estado\":\"ACTIVO\"}")).andReturn();

        assertThat(estado(sinCsrf)).isEqualTo(403);
        assertThat(estado(correoInvalido)).isEqualTo(400);
        assertThat(error(correoInvalido)).isEqualTo("ERR_SYS_01");
        assertThat(estado(campoDeMas)).isEqualTo(400);
    }

    // ---------------------------------------------------------------- restablecer

    @Test
    void restablecer_cambiaLaClaveCierraTodasLasSesionesYLoRegistra(CapturedOutput salida) throws Exception {
        Tokens web = loginWeb();
        Tokens movil = loginMobile();
        String token = pedirToken();

        MvcResult res = restablecer(token, NUEVA);

        assertThat(estado(res)).isEqualTo(200);
        assertThat(res.getResponse().getContentAsString()).contains("\"status\":\"success\"").doesNotContain("\"data\":{");
        assertThat(sesionesActivas()).isZero();
        assertAccess(web.access(), 401);
        assertAccess(movil.access(), 401);
        assertThat(estado(mvc.perform(mobile("refresh", movil.refresh())).andReturn())).isEqualTo(401);
        assertThat(login(PASSWORD, "198.51.100.20")).isEqualTo(401);
        assertThat(login(NUEVA, "198.51.100.20")).isEqualTo(200);
        assertThat(dueno().queryForObject("SELECT string_agg(DISTINCT s.motivo_revocacion, ',') FROM "
                + "authentication.sesion s JOIN authentication.usuario u ON u.id = s.usuario_id "
                + "WHERE u.email = ? AND s.estado = 'REVOCADA'", String.class, email)).isEqualTo("RESET_PASSWORD");
        assertThat(bitacora("AUTH_PASSWORD_RESTABLECIDA", "resultado")).isEqualTo("EXITO");
        assertThat(bitacora("AUTH_PASSWORD_RESTABLECIDA", "ip")).isEqualTo("127.0.0.1");
        assertThat(salida.getOut()).contains("Contraseña restablecida").doesNotContain(token).doesNotContain(NUEVA);
    }

    @Test
    void restablecer_conEnlaceUsadoReemplazadoOInventado_daERR_AUTH_05() throws Exception {
        String viejo = pedirToken();
        String vigente = pedirToken();

        assertThat(error(restablecer(viejo, NUEVA))).isEqualTo("ERR_AUTH_05");
        assertThat(error(restablecer("inventado", NUEVA))).isEqualTo("ERR_AUTH_05");
        assertThat(estado(restablecer(vigente, NUEVA))).isEqualTo(200);
        MvcResult reuso = restablecer(vigente, "TerceraClave789");
        assertThat(estado(reuso)).isEqualTo(400);
        assertThat(error(reuso)).isEqualTo("ERR_AUTH_05");
    }

    @Test
    void restablecer_conLaMismaClave_daERR_AUTH_06_yElEnlaceSigueSirviendo() throws Exception {
        String token = pedirToken();

        assertThat(error(restablecer(token, PASSWORD))).isEqualTo("ERR_AUTH_06");

        assertThat(estado(restablecer(token, NUEVA))).isEqualTo(200);
    }

    @Test
    void restablecer_conClaveCortaODeMasDe72Bytes_da400SinGastarElEnlace() throws Exception {
        String token = pedirToken();

        for (String mala : List.of("corta12", "x".repeat(73), "ñ".repeat(40))) {
            MvcResult res = restablecer(token, mala);
            assertThat(estado(res)).as(mala).isEqualTo(400);
            assertThat(error(res)).isEqualTo("ERR_SYS_01");
        }

        assertThat(tokens.consultar(token)).isPresent();
    }

    @Test
    void restablecer_conLaCuentaDesactivadaDespues_daERR_AUTH_05() throws Exception {
        String token = pedirToken();
        dueno().update("UPDATE authentication.usuario SET estado = 'INACTIVO' WHERE email = ?", email);

        assertThat(error(restablecer(token, NUEVA))).isEqualTo("ERR_AUTH_05");
    }

    @Test
    void restablecer_levantaElBloqueoDesdeCualquierIp() throws Exception {
        for (String ip : List.of("198.51.100.31", "198.51.100.32")) {
            for (int i = 0; i < 5; i++) {
                assertThat(login("ClaveEquivocada1", ip)).isEqualTo(401);
            }
            assertThat(login(PASSWORD, ip)).isEqualTo(423);
        }

        assertThat(estado(restablecer(tokens.emitir(codigoUsuario()), NUEVA))).isEqualTo(200);

        assertThat(login(NUEVA, "198.51.100.31")).isEqualTo(200);
        assertThat(login(NUEVA, "198.51.100.32")).isEqualTo(200);
    }

    @Test
    void restablecerDosVecesALaVez_soloUnaGana() throws Exception {
        String token = tokens.emitir(codigoUsuario());
        List<Callable<Integer>> llamadas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String clave = NUEVA + i;
            llamadas.add(() -> estado(restablecer(token, clave)));
        }

        List<Integer> estados = new ArrayList<>();
        try (var hilos = Executors.newFixedThreadPool(llamadas.size())) {
            for (var futuro : hilos.invokeAll(llamadas)) {
                estados.add(futuro.get());
            }
        }

        assertThat(estados).containsExactlyInAnyOrder(200, 400, 400, 400);
        assertThat(eventos("AUTH_PASSWORD_RESTABLECIDA")).isEqualTo(1);
    }

    // * Determinista: el restablecimiento completo ocurre justo después de que el
    // * login haya comparado la contraseña anterior y antes de que abra la sesión.
    @Test
    void loginConLaClaveAnterior_queCoincideConElCambio_noDejaSesion() throws Exception {
        String token = tokens.emitir(codigoUsuario());
        AtomicBoolean pendiente = new AtomicBoolean(true);
        doAnswer(llamada -> {
            Object coincide = llamada.callRealMethod();
            if (pendiente.compareAndSet(true, false)) {
                recuperacion.restablecer(token, NUEVA);
            }
            return coincide;
        }).when(passwordEncoder).matches(eq(PASSWORD), anyString());

        int resultado = login(PASSWORD, "198.51.100.40");

        assertThat(pendiente).isFalse();
        assertThat(resultado).isEqualTo(401);
        assertThat(sesionesActivas()).isZero();
        assertThat(login(NUEVA, "198.51.100.40")).isEqualTo(200);
    }

    @Test
    void loginsSimultaneosConLaClaveAnterior_noSobrevivenAlCambio() throws Exception {
        String token = tokens.emitir(codigoUsuario());
        List<Callable<Integer>> llamadas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String ip = "198.51.100." + (50 + i);
            llamadas.add(() -> login(PASSWORD, ip));
        }
        llamadas.add(() -> estado(restablecer(token, NUEVA)));

        try (var hilos = Executors.newFixedThreadPool(llamadas.size())) {
            for (var futuro : hilos.invokeAll(llamadas)) {
                assertThat(futuro.get()).isIn(200, 401);
            }
        }

        assertThat(sesionesActivas()).isZero();
    }
}
