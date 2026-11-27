package com.example.backend.features.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

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

    private Callable<Integer> quitarRol(CyclicBarrier salida, Cuenta actor, UUID objetivo) {
        return () -> {
            salida.await(10, TimeUnit.SECONDS);
            return mvc.perform(como(actor, conJson(put("/api/v1/usuarios/" + objetivo + "/roles"),
                    "{\"roles\":[\"USUARIO\"]}"))).andReturn().getResponse().getStatus();
        };
    }

    /** De las dos cuentas, cuántas siguen activas y con el rol de administrador. */
    private int administradoresEntre(Cuenta uno, Cuenta otro) {
        Integer n = dueno().queryForObject("SELECT count(*) FROM authentication.usuario u "
                + "JOIN authentication.usuario_rol ur ON ur.usuario_id = u.id AND ur.is_active "
                + "JOIN authentication.rol r ON r.id = ur.rol_id AND r.nombre = 'SUPER_ADMIN' "
                + "WHERE u.estado = 'ACTIVO' AND u.codigo_usuario IN (?, ?)", Integer.class,
                uno.codigo(), otro.codigo());
        return n == null ? 0 : n;
    }

    @Test
    void dosAdministradoresQueSeQuitanElRolALaVez_quedaExactamenteUnoConElRol() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            Cuenta uno = conSesion(crearCuenta("quitaA" + ronda, "SUPER_ADMIN"));
            Cuenta otro = conSesion(crearCuenta("quitaB" + ronda, "SUPER_ADMIN"));
            CyclicBarrier salida = new CyclicBarrier(2);

            List<Integer> estados = aLaVez(List.of(
                    quitarRol(salida, uno, otro.codigo()), quitarRol(salida, otro, uno.codigo())));

            assertThat(estados).containsExactlyInAnyOrder(200, 401);
            assertThat(administradoresEntre(uno, otro)).isEqualTo(1);
        }
    }

    @Test
    void unoQuitaElRolMientrasElOtroLoDesactiva_quedaExactamenteUnAdministrador() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            Cuenta uno = conSesion(crearCuenta("mixtoA" + ronda, "SUPER_ADMIN"));
            Cuenta otro = conSesion(crearCuenta("mixtoB" + ronda, "SUPER_ADMIN"));
            CyclicBarrier salida = new CyclicBarrier(2);

            List<Integer> estados = aLaVez(List.of(
                    quitarRol(salida, uno, otro.codigo()),
                    cambio(salida, otro, uno.codigo(), "desactivar")));

            assertThat(estados).containsExactlyInAnyOrder(200, 401);
            assertThat(administradoresEntre(uno, otro)).isEqualTo(1);
        }
    }

    @Test
    void unLoginSimultaneoNuncaConservaLosRolesAnteriores() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            Cuenta persona = crearCuenta("cruce" + ronda, "SUPER_ADMIN");
            CyclicBarrier salida = new CyclicBarrier(2);
            AtomicReference<MockHttpServletResponse> entrada = new AtomicReference<>();
            Callable<Integer> iniciar = () -> {
                salida.await(10, TimeUnit.SECONDS);
                entrada.set(login(persona.email(), PASSWORD).andReturn().getResponse());
                return entrada.get().getStatus();
            };

            List<Integer> estados = aLaVez(List.of(iniciar, quitarRol(salida, admin, persona.codigo())));

            assertThat(estados).containsExactly(200, 200);
            // * Si entró antes del cambio, su sesión quedó revocada; si entró después, ya sin el rol.
            MvcResult yo = mvc.perform(get("/api/v1/auth/me").cookie(entrada.get().getCookie("accessToken")))
                    .andReturn();
            if (yo.getResponse().getStatus() == 200) {
                assertThat(yo.getResponse().getContentAsString()).doesNotContain("SUPER_ADMIN");
            } else {
                assertThat(yo.getResponse().getStatus()).isEqualTo(401);
            }
        }
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
