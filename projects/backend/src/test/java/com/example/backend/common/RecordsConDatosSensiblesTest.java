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
 * Ningún record con secretos o datos personales los muestra en su `toString()`
 * (lo usan Spring y cualquier log que reciba el objeto). Recorre todo el código:
 * un record nuevo entra solo, sin tocar esta prueba.
 *
 * * Los reconoce por el nombre del componente. Un dato con otro nombre no se
 * * detecta: se nombra como lo que es.
 */
class RecordsConDatosSensiblesTest {

    private static final Pattern SENSIBLE = Pattern.compile(
            // Secretos: en cualquier parte del nombre. Datos personales: el nombre entero.
            "(?i).*(password|contrasena|clave|token|secret|credencial).*|email|correo|ip|useragent|search|texto");
    private static final String MARCA = "SondaQueNoDebeSalir"; // gitleaks:allow

    @Test
    void ningunRecordConDatosSensibles_losMuestraEnSuToString() throws Exception {
        List<String> revisados = new ArrayList<>();
        List<String> expuestos = new ArrayList<>();
        for (Class<?> tipo : recordsDeLaAplicacion()) {
            RecordComponent[] componentes = tipo.getRecordComponents();
            if (Arrays.stream(componentes).noneMatch(RecordsConDatosSensiblesTest::esSensible)) {
                continue;
            }
            revisados.add(tipo.getSimpleName());
            if (conMarcas(tipo, componentes).toString().contains(MARCA)) {
                expuestos.add(tipo.getSimpleName());
            }
        }

        // Si la búsqueda dejara de encontrarlos, la prueba pasaría sin comprobar nada.
        assertThat(revisados).contains("LoginRequest", "LoginResponse", "LoginResult", "SesionCreada",
                "CambiarPasswordRequest", "UsuarioResponse", "SesionResponse", "OrigenSolicitud");
        assertThat(expuestos).as("records que muestran un dato sensible: redefine toString()").isEmpty();
    }

    private static boolean esSensible(RecordComponent componente) {
        return componente.getType() == String.class && SENSIBLE.matcher(componente.getName()).matches();
    }

    /** Una instancia con la marca en cada dato sensible y el resto vacío. */
    private static Object conMarcas(Class<?> tipo, RecordComponent[] componentes) throws Exception {
        Class<?>[] tipos = Arrays.stream(componentes).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Object[] valores = Arrays.stream(componentes).map(c -> esSensible(c) ? MARCA : vacio(c.getType())).toArray();
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
