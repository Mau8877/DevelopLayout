package com.example.backend.common.idioma;

import java.util.Locale;

import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Elige el idioma solo a partir de `Accept-Language`.
 *
 * ! No usa `request.getLocale()` (ni AcceptHeaderLocaleResolver, que se apoya en
 * ! él): ante una cabecera mal formada devuelve el idioma del SERVIDOR, y uno
 * ! configurado en inglés respondería en inglés sin que nadie lo pidiera.
 * * El texto del cliente solo elige entre los dos idiomas soportados.
 */
public class SelectorDeIdioma implements LocaleResolver {

    private static final int LARGO_MAXIMO = 200;

    /** El primero soportado según las preferencias; si no hay ninguno, español. */
    public static Locale elegir(String cabecera) {
        if (cabecera == null || cabecera.isBlank() || cabecera.length() > LARGO_MAXIMO) {
            return Mensajes.ESPANOL;
        }
        try {
            for (Locale.LanguageRange preferencia : Locale.LanguageRange.parse(cabecera)) {
                if (preferencia.getWeight() <= 0) {
                    continue;
                }
                String idioma = preferencia.getRange().split("-", 2)[0];
                if (Mensajes.ESPANOL.getLanguage().equals(idioma)) {
                    return Mensajes.ESPANOL;
                }
                if (Mensajes.INGLES.getLanguage().equals(idioma)) {
                    return Mensajes.INGLES;
                }
            }
        } catch (IllegalArgumentException e) {
            // Cabecera mal formada: se responde en el idioma por defecto.
        }
        return Mensajes.ESPANOL;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return elegir(request.getHeader("Accept-Language"));
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("El idioma lo decide la cabecera Accept-Language");
    }
}
