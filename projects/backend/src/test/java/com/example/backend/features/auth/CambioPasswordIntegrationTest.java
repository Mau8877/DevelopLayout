package com.example.backend.features.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.backend.config.RecuperacionEjecutorConfig;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Apoyo común de las pruebas del cambio de contraseña. */
abstract class CambioPasswordIntegrationTest extends AuthSesionIntegrationTest {

    protected static final String NUEVA = "OtraClaveNueva456"; // gitleaks:allow
    protected static final String OTRA = "ClaveEquivocada789"; // gitleaks:allow

    @MockitoBean
    protected JavaMailSender mail;

    @Autowired
    @Qualifier(RecuperacionEjecutorConfig.EJECUTOR)
    private ThreadPoolTaskExecutor ejecutor;

    protected final List<SimpleMailMessage> bandeja = new CopyOnWriteArrayList<>();

    /** Una IP por prueba: el límite por IP no se comparte entre pruebas. */
    protected String ip;

    @BeforeEach
    void prepararCorreoEIp() {
        bandeja.clear();
        doAnswer(llamada -> bandeja.add(llamada.getArgument(0))).when(mail).send(any(SimpleMailMessage.class));
        ThreadLocalRandom azar = ThreadLocalRandom.current();
        ip = "10." + azar.nextInt(256) + "." + azar.nextInt(256) + "." + azar.nextInt(1, 255);
    }

    protected static String cuerpo(String actual, String nueva) {
        return "{\"passwordActual\":\"" + actual + "\",\"passwordNueva\":\"" + nueva + "\"}";
    }

    private MockHttpServletRequestBuilder desdeEstaPrueba(MockHttpServletRequestBuilder peticion) {
        return peticion.header("User-Agent", "navegador-de-prueba").with(r -> {
            r.setRemoteAddr(ip);
            return r;
        });
    }

    protected MockHttpServletRequestBuilder cambioWeb(Tokens tokens, String json) {
        return desdeEstaPrueba(post("/api/v1/auth/password/change").cookie(new Cookie("accessToken", tokens.access()))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected MockHttpServletRequestBuilder cambioMobile(Tokens tokens, String json) {
        return desdeEstaPrueba(post("/api/v1/auth/password/change").header("X-Client-Type", "mobile")
                .header("Authorization", "Bearer " + tokens.access())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected MvcResult cambiar(Tokens tokens, String actual, String nueva) throws Exception {
        return mvc.perform(cambioMobile(tokens, cuerpo(actual, nueva))).andReturn();
    }

    protected static int estado(MvcResult res) {
        return res.getResponse().getStatus();
    }

    protected static String error(MvcResult res) throws Exception {
        return JsonPath.read(res.getResponse().getContentAsString(), "$.error");
    }

    protected int me(Tokens tokens) throws Exception {
        return estado(mvc.perform(get("/api/v1/auth/me").header("X-Client-Type", "mobile")
                .header("Authorization", "Bearer " + tokens.access()))
                .andReturn());
    }

    protected int renovar(Tokens tokens) throws Exception {
        return estado(mvc.perform(desdeEstaPrueba(post("/api/v1/auth/refresh").header("X-Client-Type", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.refresh() + "\"}"))).andReturn());
    }

    protected int login(String password) throws Exception {
        return estado(mvc.perform(desdeEstaPrueba(post("/api/v1/auth/login").header("X-Client-Type", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))).andReturn());
    }

    protected UUID codigoUsuario() {
        return UUID.fromString(dueno().queryForObject(
                "SELECT codigo_usuario::text FROM authentication.usuario WHERE email = ?", String.class, email));
    }

    protected String hashGuardado() {
        return dueno().queryForObject("SELECT password_hash FROM authentication.usuario WHERE email = ?",
                String.class, email);
    }

    protected int sesionesActivas() {
        return dueno().queryForObject("SELECT count(*) FROM authentication.sesion s JOIN authentication.usuario u "
                + "ON u.id = s.usuario_id WHERE u.email = ? AND s.estado = 'ACTIVA'", Integer.class, email);
    }

    protected List<String> motivos() {
        return dueno().queryForList("SELECT s.motivo_revocacion FROM authentication.sesion s "
                + "JOIN authentication.usuario u ON u.id = s.usuario_id WHERE u.email = ? "
                + "AND s.estado = 'REVOCADA'", String.class, email);
    }

    protected String bitacora(String accion, String columna) {
        return dueno().queryForObject("SELECT string_agg(coalesce(b." + columna + "::text, ''), '|') "
                + "FROM authentication.bitacora b JOIN authentication.usuario u ON u.codigo_usuario = b.usuario_codigo "
                + "WHERE u.email = ? AND b.accion = ?", String.class, email, accion);
    }

    protected void esperarEjecutorLibre() {
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15))
                .until(() -> ejecutor.getThreadPoolExecutor().getQueue().isEmpty() && ejecutor.getActiveCount() == 0);
    }
}
