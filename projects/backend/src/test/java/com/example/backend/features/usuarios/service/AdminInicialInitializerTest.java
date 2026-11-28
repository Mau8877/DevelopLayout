package com.example.backend.features.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.backend.support.PostgresIntegrationTest;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class AdminInicialInitializerTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AdminInicialInitializer initializer;

    @Test
    void alArrancar_conBaseVacia_creaElSuperAdminConHashBcrypt() {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM authentication.usuario WHERE email = 'admin@test.local'", Integer.class);
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM authentication.usuario WHERE email = 'admin@test.local'", String.class);
        Integer roles = jdbc.queryForObject(
                "SELECT count(*) FROM authentication.usuario_rol ur "
                        + "JOIN authentication.rol r ON r.id = ur.rol_id WHERE r.nombre = 'SUPER_ADMIN'",
                Integer.class);

        assertThat(total).isEqualTo(1);
        assertThat(hash).startsWith("$2");
        assertThat(roles).isEqualTo(1);
    }

    @Test
    void elAlta_registraElCodigoDelUsuarioYNoSuCorreo(CapturedOutput salida) {
        // * Base descartable: se quita el administrador para volver a ver su alta.
        jdbc.update("DELETE FROM authentication.usuario_rol WHERE usuario_id IN "
                + "(SELECT id FROM authentication.usuario WHERE email = 'admin@test.local')");
        jdbc.update("DELETE FROM authentication.usuario WHERE email = 'admin@test.local'");

        initializer.run(null);

        String codigo = jdbc.queryForObject("SELECT codigo_usuario::text FROM authentication.usuario "
                + "WHERE email = 'admin@test.local'", String.class);
        assertThat(salida.getAll()).contains("Administrador inicial creado usuarioCodigo=" + codigo)
                .doesNotContain("admin@test.local");
    }

    @Test
    void ejecutarDeNuevo_conAdminExistente_noCambiaNada() {
        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();

        Integer usuarios = jdbc.queryForObject("SELECT count(*) FROM authentication.usuario", Integer.class);
        assertThat(usuarios).isEqualTo(1);
    }

    @Test
    void validar_sinCredenciales_lanzaMensajeClaro() {
        assertThatThrownBy(() -> AdminInicialInitializer.validar("", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INITIAL_ADMIN_PASSWORD");
    }

    @Test
    void validar_conUnaContrasenaMuyUsada_lanzaExcepcionSinMostrarla() {
        String comun = "Password123"; // gitleaks:allow

        assertThatThrownBy(() -> AdminInicialInitializer.validar("admin@test.local", comun))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("muy usada")
                .hasMessageNotContaining(comun);
    }

    @Test
    void validar_conCorreoInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> AdminInicialInitializer.validar("no-es-correo", "ClaveValida123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("correo");
    }

    @Test
    void validar_conContrasenaCortaOLarga_lanzaExcepcionSinMostrarla() {
        assertThatThrownBy(() -> AdminInicialInitializer.validar("a@b.co", "corta"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("corta");
        assertThatThrownBy(() -> AdminInicialInitializer.validar("a@b.co", "x".repeat(73)))
                .isInstanceOf(IllegalStateException.class);
    }
}
