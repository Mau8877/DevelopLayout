package com.example.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Ningún record con secretos los muestra en su `toString()` (lo usan Spring y
 * cualquier log que reciba el objeto). Recorre todo el código: un record nuevo
 * entra solo, sin tocar esta prueba.
 *
 * * Los reconoce por el nombre del componente. Un secreto con otro nombre no se
 * * detecta: se nombra como lo que es.
 */
class RecordsConSecretosTest {

    private static final Pattern SECRETO = Pattern.compile("(?i)password|contrasena|clave|token|secret|credencial");
    private static final String MARCA = "SondaQueNoDebeSalir"; // gitleaks:allow

    @Test
    void ningunRecordConSecretos_losMuestraEnSuToString() throws Exception {
        List<String> revisados = new ArrayList<>();
        List<String> expuestos = new ArrayList<>();
        for (Class<?> tipo : recordsDeLaAplicacion()) {
            RecordComponent[] componentes = tipo.getRecordComponents();
            if (Arrays.stream(componentes).noneMatch(RecordsConSecretosTest::esSecreto)) {
                continue;
            }
            revisados.add(tipo.getSimpleName());
            if (conMarcas(tipo, componentes).toString().contains(MARCA)) {
                expuestos.add(tipo.getSimpleName());
            }
        }

        // Si la búsqueda dejara de encontrarlos, la prueba pasaría sin comprobar nada.
        assertThat(revisados).contains("LoginRequest", "LoginResponse", "LoginResult", "SesionCreada",
                "CambiarPasswordRequest");
        assertThat(expuestos).as("records que muestran un secreto: redefine toString()").isEmpty();
    }

    private static boolean esSecreto(RecordComponent componente) {
        return componente.getType() == String.class && SECRETO.matcher(componente.getName()).find();
    }

    /** Una instancia con la marca en cada secreto y el resto vacío. */
    private static Object conMarcas(Class<?> tipo, RecordComponent[] componentes) throws Exception {
        Class<?>[] tipos = Arrays.stream(componentes).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Object[] valores = Arrays.stream(componentes).map(c -> esSecreto(c) ? MARCA : vacio(c.getType())).toArray();
        Constructor<?> constructor = tipo.getDeclaredConstructor(tipos);
        constructor.setAccessible(true);
        return constructor.newInstance(valores);
    }

    private static Object vacio(Class<?> tipo) {
        if (!tipo.isPrimitive()) {
            return null;
        }
        return tipo == boolean.class ? Boolean.FALSE : 0;
    }

    private static List<Class<?>> recordsDeLaAplicacion() {
        var buscador = new ClassPathScanningCandidateComponentProvider(false);
        buscador.addIncludeFilter(new AssignableTypeFilter(Record.class));
        List<Class<?>> records = new ArrayList<>();
        for (var definicion : buscador.findCandidateComponents("com.example.backend")) {
            Class<?> tipo = ClassUtils.resolveClassName(definicion.getBeanClassName(), null);
            URL origen = tipo.getProtectionDomain().getCodeSource().getLocation();
            if (!origen.getPath().contains("test-classes")) {
                records.add(tipo);
            }
        }
        return records;
    }
}
