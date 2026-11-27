package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/** Cruces entre administradores: siempre queda uno que puede entrar (CA16). */
class AdministradoresConcurrenciaTest extends AdminUsuariosIntegrationTest {

    // * Varias rondas: un cruce que solo a veces se solapa tiene que aparecer.
    private static final int RONDAS = 6;

    private Callable<Integer> cambio(CyclicBarrier salida, Cuenta actor, UUID objetivo, String accion) {
        return () -> {
            salida.await(10, TimeUnit.SECONDS);
            return mvc.perform(como(actor, post("/api/v1/usuarios/" + objetivo + "/" + accion)))
                    .andReturn().getResponse().getStatus();
        };
    }

    private List<Integer> aLaVez(List<Callable<Integer>> cambios) throws Exception {
        List<Integer> estados = new ArrayList<>();
        try (var hilos = Executors.newFixedThreadPool(cambios.size())) {
            for (var futuro : hilos.invokeAll(cambios)) {
                estados.add(futuro.get());
            }
        }
        return estados;
    }

    @Test
    void dosAdministradoresQueSeDesactivanALaVez_quedaExactamenteUnoActivo() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            Cuenta uno = conSesion(crearCuenta("uno" + ronda, "SUPER_ADMIN"));
            Cuenta otro = conSesion(crearCuenta("otro" + ronda, "SUPER_ADMIN"));
            CyclicBarrier salida = new CyclicBarrier(2);

            List<Integer> estados = aLaVez(List.of(
                    cambio(salida, uno, otro.codigo(), "desactivar"),
                    cambio(salida, otro, uno.codigo(), "desactivar")));

            assertThat(estados).containsExactlyInAnyOrder(200, 401);
            assertThat(List.of(columna(uno.codigo(), "estado"), columna(otro.codigo(), "estado")))
                    .containsExactlyInAnyOrder("ACTIVO", "INACTIVO");
        }
    }
}
