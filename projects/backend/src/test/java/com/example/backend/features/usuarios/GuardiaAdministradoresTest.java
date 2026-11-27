package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.features.usuarios.service.GuardiaAdministradores;

class GuardiaAdministradoresTest extends AdminUsuariosIntegrationTest {

    @Autowired
    private GuardiaAdministradores guardia;

    @Autowired
    private PlatformTransactionManager transacciones;

    private UUID sesionDe(Cuenta cuenta) {
        return dueno().queryForObject("SELECT codigo_sesion FROM authentication.sesion WHERE usuario_codigo = ?",
                UUID.class, cuenta.codigo());
    }

    private void exigir(UUID actor, UUID sesion) {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> guardia.exigirVigente(actor, sesion));
    }

    @Test
    void dejaPasarAUnAdministradorActivoConSuSesion() {
        assertThatCode(() -> exigir(admin.codigo(), sesionDe(admin))).doesNotThrowAnyException();
    }

    @Test
    void deniegaSiLaCuentaEstaInactiva() {
        dueno().update("UPDATE authentication.usuario SET estado = 'INACTIVO' WHERE codigo_usuario = ?",
                admin.codigo());

        assertThatThrownBy(() -> exigir(admin.codigo(), sesionDe(admin))).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deniegaSiYaNoTieneElRol() {
        dueno().update("UPDATE authentication.usuario_rol SET is_active = false WHERE usuario_id = "
                + "(SELECT id FROM authentication.usuario WHERE codigo_usuario = ?)", admin.codigo());

        assertThatThrownBy(() -> exigir(admin.codigo(), sesionDe(admin))).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deniegaSiSuSesionFueRevocadaOEsDeOtro() throws Exception {
        Cuenta otro = conSesion(crearCuenta("otro", "SUPER_ADMIN"));
        UUID propia = sesionDe(admin);

        assertThatThrownBy(() -> exigir(admin.codigo(), sesionDe(otro))).isInstanceOf(UnauthorizedException.class);
        dueno().update("UPDATE authentication.sesion SET estado = 'REVOCADA' WHERE codigo_sesion = ?", propia);
        assertThatThrownBy(() -> exigir(admin.codigo(), propia)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void elSegundoEsperaAlPrimeroYVeLoQueConfirmo() throws Exception {
        Cuenta segundo = conSesion(crearCuenta("segundo", "SUPER_ADMIN"));
        UUID sesionPrimero = sesionDe(admin);
        UUID sesionSegundo = sesionDe(segundo);
        CountDownLatch conElTurno = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);

        try (var hilos = Executors.newFixedThreadPool(2)) {
            var primero = hilos.submit(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                guardia.exigirVigente(admin.codigo(), sesionPrimero);
                // * Mientras tiene el turno, deja al segundo sin ser administrador.
                dueno().update("UPDATE authentication.sesion SET estado = 'REVOCADA' WHERE codigo_sesion = ?",
                        sesionSegundo);
                conElTurno.countDown();
                esperar(soltar);
            }));
            esperar(conElTurno);
            var intento = hilos.submit(() -> exigir(segundo.codigo(), sesionSegundo));

            assertThatThrownBy(() -> intento.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            soltar.countDown();
            primero.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> intento.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(UnauthorizedException.class);
        }
    }

    @Test
    void sinTransaccionFalla() {
        assertThatThrownBy(() -> guardia.exigirVigente(admin.codigo(), sesionDe(admin)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private static void esperar(CountDownLatch senal) {
        try {
            if (!senal.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("La señal no llegó");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
