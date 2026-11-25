package com.example.backend.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.support.PostgresRuntimeIntegrationTest;
import com.jayway.jsonpath.JsonPath;

/**
 * Contrato de la API: lo que el backend publica en /v3/api-docs tiene que coincidir con el
 * snapshot versionado en `docs/api/openapi.json`, de donde el frontend genera sus tipos.
 *
 * * Si falla, el contrato cambió: se regenera con `pnpm openapi:sync` (backend de dev
 * * levantado), se revisa el diff y se commitea el snapshot junto con el cambio del backend.
 * * `servers` se ignora: depende de desde dónde se pida el documento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiContractTest extends PostgresRuntimeIntegrationTest {

    private static final Path SNAPSHOT = Path.of("..", "..", "docs", "api", "openapi.json");

    @Autowired
    private MockMvc mvc;

    @Test
    void contratoPublicado_coincideConElSnapshotVersionado() throws Exception {
        assertThat(SNAPSHOT)
                .as("falta el snapshot del contrato: corré `pnpm openapi:sync` con el backend de dev levantado")
                .exists();

        String vivo = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String versionado = Files.readString(SNAPSHOT, StandardCharsets.UTF_8);

        Map<String, Object> contratoVivo = sinServers(vivo);
        Map<String, Object> contratoVersionado = sinServers(versionado);

        assertThat(contratoVivo)
                .as("el contrato de la API cambió respecto a docs/api/openapi.json: corré `pnpm openapi:sync`")
                .isEqualTo(contratoVersionado);
    }

    /** El snapshot ya viene sin `servers` y el documento en vivo sí: se quita sin exigir que exista. */
    private static Map<String, Object> sinServers(String json) {
        Map<String, Object> documento = JsonPath.parse(json).read("$");
        documento.remove("servers");
        return documento;
    }
}
