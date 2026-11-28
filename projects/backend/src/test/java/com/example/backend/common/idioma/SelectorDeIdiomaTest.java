package com.example.backend.common.idioma;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SelectorDeIdiomaTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "en                              | en",
        "en-US,en;q=0.9,es;q=0.8         | en",
        "es-BO,es;q=0.9,en;q=0.8         | es",
        "fr-FR,en;q=0.5                  | en",
        "es;q=0.3,en;q=0.7               | en",
        "en;q=0,es                       | es",
        "fr-FR                           | es",
        "*                               | es",
        "''                              | es",
        "../../etc/passwd                | es",
        "en;q=no-es-numero               | es",
    })
    void eligeSoloEntreLosIdiomasSoportados(String cabecera, String esperado) {
        assertThat(SelectorDeIdioma.elegir(cabecera).getLanguage()).isEqualTo(esperado);
    }

    @Test
    void noDependeDelIdiomaDelServidor_niAceptaCabecerasEnormes() {
        Locale anterior = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertThat(SelectorDeIdioma.elegir(null)).isEqualTo(Mensajes.ESPANOL);
            assertThat(SelectorDeIdioma.elegir("!!!")).isEqualTo(Mensajes.ESPANOL);
            assertThat(new Mensajes(Mensajes.crearFuente()).en(Mensajes.ESPANOL, "common.error.accesoDenegado"))
                    .isEqualTo("Acceso denegado");
            assertThat(SelectorDeIdioma.elegir("en,".repeat(200))).isEqualTo(Mensajes.ESPANOL);
        } finally {
            Locale.setDefault(anterior);
        }
    }
}
