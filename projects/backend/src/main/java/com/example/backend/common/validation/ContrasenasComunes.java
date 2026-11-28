package com.example.backend.common.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contraseñas muy usadas que aparecen en filtraciones.
 *
 * * La comprobación es local (la contraseña no sale del servidor) y no
 * * distingue mayúsculas. Origen y licencia de la lista:
 * * resources/security/ORIGEN.md.
 * ! Si la lista falta o está vacía, falla en vez de aceptarlo todo.
 */
public final class ContrasenasComunes {

    private static final String RECURSO = "/security/contrasenas-comunes.txt";
    private static final Set<String> LISTA = cargar();

    private ContrasenasComunes() {
    }

    public static boolean contiene(String password) {
        return LISTA.contains(password.toLowerCase(Locale.ROOT));
    }

    static int cantidad() {
        return LISTA.size();
    }

    private static Set<String> cargar() {
        try (InputStream entrada = ContrasenasComunes.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta el recurso " + RECURSO);
            }
            Set<String> lista = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toUnmodifiableSet());
            if (lista.isEmpty()) {
                throw new IllegalStateException("El recurso " + RECURSO + " está vacío");
            }
            return lista;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
