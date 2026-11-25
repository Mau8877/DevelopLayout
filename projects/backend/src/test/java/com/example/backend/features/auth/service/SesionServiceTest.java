package com.example.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.common.util.Sha256;
import com.example.backend.features.auth.enums.TipoCliente;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.repository.UsuarioRepository;
import com.example.backend.security.RlsContext;
import com.example.backend.support.PostgresRuntimeIntegrationTest;

@SpringBootTest
class SesionServiceTest extends PostgresRuntimeIntegrationTest {

    @Autowired
    private SesionService sesionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RlsContext rlsContext;

    @Autowired
    private PlatformTransactionManager tm;

    private SesionService.SesionCreada crear() {
        return new TransactionTemplate(tm).execute(status -> {
            rlsContext.usarCorreoDeLogin("admin@test.local");
            Usuario u = usuarioRepository.findByEmailIgnoreCase("admin@test.local").orElseThrow();
            rlsContext.usarUsuario(u.getCodigo());
            return sesionService.crear(u, TipoCliente.WEB, "10.0.0.9", "JUnit");
        });
    }

    @Test
    void crear_dejaSesionActivaConElHashDelToken() {
        SesionService.SesionCreada creada = crear();

        Map<String, Object> fila = dueno().queryForMap(
                "SELECT refresh_hash, estado, tipo_cliente, ip, expira_en > now() AS vigente "
                        + "FROM authentication.sesion WHERE codigo_sesion = ?", creada.sesion().getCodigo());
        assertThat(fila.get("refresh_hash")).isEqualTo(Sha256.hex(creada.refreshToken()));
        assertThat(fila.get("estado")).isEqualTo("ACTIVA");
        assertThat(fila.get("tipo_cliente")).isEqualTo("WEB");
        assertThat(fila.get("ip")).isEqualTo("10.0.0.9");
        assertThat(fila.get("vigente")).isEqualTo(true);
    }

    @Test
    void crear_dosVecesDaTokensDistintosYNingunoEnClaroEnLaBase() {
        SesionService.SesionCreada a = crear();
        SesionService.SesionCreada b = crear();

        assertThat(a.refreshToken()).isNotEqualTo(b.refreshToken());
        List<String> hashes = dueno().queryForList("SELECT refresh_hash FROM authentication.sesion", String.class);
        assertThat(hashes).doesNotContain(a.refreshToken(), b.refreshToken());
    }
}
