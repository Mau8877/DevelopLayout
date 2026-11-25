package com.example.backend.common.bitacora;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.backend.support.PostgresRuntimeIntegrationTest;

/**
 * La app corre como `app_runtime` (como en producción); las lecturas de
 * verificación usan la conexión dueña porque `bitacora` solo se lee con
 * `app.is_admin`. Si Docker no está disponible, el test queda SKIPPED.
 */
@SpringBootTest
class BitacoraServiceTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private BitacoraService bitacoraService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private int contarPorAccion(String accion) {
        Integer total = dueno().queryForObject(
                "SELECT count(*) FROM authentication.bitacora WHERE accion = ?", Integer.class, accion);
        return total == null ? 0 : total;
    }

    @AfterEach
    void limpiarRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void registrar_insertaUnaFilaConActorIpYUserAgent() {
        String accion = "TEST_" + UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID entidadCodigo = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("User-Agent", "JUnit/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        bitacoraService.registrar(actor, accion, "auth", "sesion", entidadCodigo, ResultadoBitacora.EXITO,
                Map.of("campo", "valor", "campos", List.of("a", "b")));

        Map<String, Object> fila = dueno().queryForMap(
                "SELECT usuario_codigo, modulo, entidad, entidad_codigo, resultado, ip, user_agent, "
                        + "detalle->>'campo' AS campo, detalle->'campos'->>1 AS segundo "
                        + "FROM authentication.bitacora WHERE accion = ?", accion);
        assertThat(fila.get("usuario_codigo")).isEqualTo(actor);
        assertThat(fila.get("modulo")).isEqualTo("auth");
        assertThat(fila.get("entidad")).isEqualTo("sesion");
        assertThat(fila.get("entidad_codigo")).isEqualTo(entidadCodigo);
        assertThat(fila.get("resultado")).isEqualTo("EXITO");
        assertThat(fila.get("ip")).isEqualTo("10.0.0.5");
        assertThat(fila.get("user_agent")).isEqualTo("JUnit/1.0");
        assertThat(fila.get("campo")).isEqualTo("valor");
        assertThat(fila.get("segundo")).isEqualTo("b");
    }

    @Test
    void registrar_sinRequestNiActor_dejaActorIpYUserAgentEnNull() {
        String accion = "TEST_" + UUID.randomUUID();
        // * @SpringBootTest (entorno MOCK) deja un request simulado (127.0.0.1) en el
        // * RequestContextHolder; para probar "sin request" hay que quitarlo a propósito.
        RequestContextHolder.resetRequestAttributes();

        bitacoraService.registrar(accion, "auth", null, null, ResultadoBitacora.FALLO, null);

        Map<String, Object> fila = dueno().queryForMap(
                "SELECT usuario_codigo, ip, user_agent, detalle FROM authentication.bitacora WHERE accion = ?",
                accion);
        assertThat(fila.get("usuario_codigo")).isNull();
        assertThat(fila.get("ip")).isNull();
        assertThat(fila.get("user_agent")).isNull();
        assertThat(fila.get("detalle")).isNull();
    }

    @Test
    void registrar_truncaElUserAgentA255() {
        String accion = "TEST_" + UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "x".repeat(400));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        bitacoraService.registrar(accion, "auth", null, null, ResultadoBitacora.EXITO, null);

        Integer largo = dueno().queryForObject(
                "SELECT length(user_agent) FROM authentication.bitacora WHERE accion = ?", Integer.class, accion);
        assertThat(largo).isEqualTo(255);
    }

    @Test
    void registrar_conClaveDePasswordEnElDetalle_lanzaExcepcionYNoInserta() {
        String accion = "TEST_" + UUID.randomUUID();

        assertThatThrownBy(() -> bitacoraService.registrar(accion, "auth", null, null, ResultadoBitacora.EXITO,
                Map.of("password", "secreta")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(contarPorAccion(accion)).isZero();
    }

    @Test
    void sanitizar_rechazaClavesDeSecretosYValoresComplejos() {
        for (String clave : List.of("Token", "refreshHash", "AUTHORIZATION", "secretKey", "cardNumber")) {
            assertThatThrownBy(() -> BitacoraService.sanitizar(Map.of(clave, "x")))
                    .as(clave).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> BitacoraService.sanitizar(Map.of("objeto", Map.of("a", 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitacoraService.sanitizar(Map.of("lista", List.of(1, 2))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serializarDetalle_escapaComillasBarrasYSaltosDeLinea() {
        String json = BitacoraService.serializarDetalle(Map.of("k", "a\"b\\c\nd"));

        assertThat(json).isEqualTo("{\"k\":\"a\\\"b\\\\c\\nd\"}");
    }

    @Test
    void registrarEnTransaccionPropia_conservaElEventoAunqueLaOperacionHagaRollback() {
        String accion = "TEST_" + UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            bitacoraService.registrarEnTransaccionPropia(accion, "auth", null, null,
                    ResultadoBitacora.FALLO, null);
            status.setRollbackOnly();
        });

        assertThat(contarPorAccion(accion)).isEqualTo(1);
    }

    @Test
    void registrar_seUneALaTransaccionYSePierdeConElRollback() {
        String accion = "TEST_" + UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            bitacoraService.registrar(accion, "auth", null, null, ResultadoBitacora.EXITO, null);
            status.setRollbackOnly();
        });

        assertThat(contarPorAccion(accion)).isZero();
    }
}
