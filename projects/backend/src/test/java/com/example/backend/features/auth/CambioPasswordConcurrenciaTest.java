package com.example.backend.features.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Carreras del cambio de contraseña. El cruce se fuerza deteniendo el cálculo o
 * la comprobación de BCrypt, que ocurre entre las dos transacciones.
 */
class CambioPasswordConcurrenciaTest extends CambioPasswordIntegrationTest {

    private static final String SEGUNDA = "SegundaClaveNueva789"; // gitleaks:allow

    @MockitoSpyBean
    private PasswordEncoder passwordEncoder;

    @Test
    void dosCambiosSimultaneos_soloUnoTieneExito() throws Exception {
        Tokens primera = loginMobile();
        Tokens segunda = loginMobile();
        // * Los dos ya verificaron la contraseña actual antes de que ninguno escriba.
        CyclicBarrier juntos = new CyclicBarrier(2);
        doAnswer(llamada -> {
            juntos.await(20, TimeUnit.SECONDS);
            return llamada.callRealMethod();
        }).when(passwordEncoder).encode(any());

        try (var hilos = Executors.newFixedThreadPool(2)) {
            var a = hilos.submit(() -> estado(cambiar(primera, PASSWORD, NUEVA)));
            var b = hilos.submit(() -> estado(cambiar(segunda, PASSWORD, SEGUNDA)));

            assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 401);
        }

        assertThat(sesionesActivas()).isZero();
    }

    @Test
    void sesionCerradaMientrasSeProcesa_noCambiaLaContrasena() throws Exception {
        Tokens tokens = loginMobile();
        String hashAnterior = hashGuardado();
        doAnswer(llamada -> {
            dueno().update("UPDATE authentication.sesion SET estado = 'REVOCADA', motivo_revocacion = 'ADMIN' "
                    + "WHERE usuario_codigo = ?", codigoUsuario());
            return llamada.callRealMethod();
        }).when(passwordEncoder).encode(any());

        var res = cambiar(tokens, PASSWORD, NUEVA);

        assertThat(estado(res)).isEqualTo(401);
        assertThat(error(res)).isEqualTo("ERR_AUTH_04");
        assertThat(hashGuardado()).isEqualTo(hashAnterior);
    }

    @Test
    void loginConLaContrasenaAnteriorQueTerminaAntes_quedaRevocado() throws Exception {
        Tokens tokens = loginMobile();
        doAnswer(llamada -> {
            // * En mitad del cambio entra alguien con la contraseña anterior.
            assertThat(login(PASSWORD)).isEqualTo(200);
            return llamada.callRealMethod();
        }).when(passwordEncoder).encode(NUEVA);

        assertThat(estado(cambiar(tokens, PASSWORD, NUEVA))).isEqualTo(200);

        assertThat(sesionesActivas()).isZero();
    }

    @Test
    void loginConLaContrasenaAnteriorQueTerminaDespues_noAbreSesion() throws Exception {
        Tokens tokens = loginMobile();
        CountDownLatch verificado = new CountDownLatch(1);
        CountDownLatch cambioHecho = new CountDownLatch(1);
        Thread[] hiloDelLogin = new Thread[1];
        doAnswer(llamada -> {
            Object resultado = llamada.callRealMethod();
            if (Thread.currentThread() == hiloDelLogin[0]) {
                verificado.countDown();
                assertThat(cambioHecho.await(20, TimeUnit.SECONDS)).isTrue();
            }
            return resultado;
        }).when(passwordEncoder).matches(any(), any());

        try (var hilos = Executors.newSingleThreadExecutor()) {
            var loginTardio = hilos.submit(() -> {
                hiloDelLogin[0] = Thread.currentThread();
                return login(PASSWORD);
            });
            assertThat(verificado.await(20, TimeUnit.SECONDS)).isTrue();

            assertThat(estado(cambiar(tokens, PASSWORD, NUEVA))).isEqualTo(200);
            cambioHecho.countDown();

            assertThat(loginTardio.get()).isEqualTo(401);
        }
        assertThat(sesionesActivas()).isZero();
    }
}
