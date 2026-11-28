package com.example.backend.common.idioma;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Único punto para traducir una clave a lo que ve la persona.
 *
 * * El idioma del cliente solo ELIGE entre los soportados: nunca se usa para
 * * armar una ruta ni un texto.
 * ! Una clave inexistente lanza una excepción: no se le muestra la clave a nadie.
 */
@Component
public class Mensajes {

    public static final Locale ESPANOL = Locale.forLanguageTag("es");
    public static final Locale INGLES = Locale.ENGLISH;

    private final MessageSource fuente;

    public Mensajes(MessageSource messageSource) {
        this.fuente = messageSource;
    }

    /** Los paquetes de mensajes, uno por módulo (resources/i18n/{modulo}/). */
    public static ResourceBundleMessageSource crearFuente() {
        ResourceBundleMessageSource fuente = new ResourceBundleMessageSource();
        fuente.setBasenames("i18n/common/messages", "i18n/auth/messages", "i18n/usuarios/messages",
                "i18n/bitacora/messages");
        fuente.setDefaultEncoding("UTF-8");
        // * Sin esto, un servidor configurado en inglés respondería en inglés a quien pide español.
        fuente.setFallbackToSystemLocale(false);
        return fuente;
    }

    /** El idioma de la petición en curso; fuera de una petición, español. */
    public Locale idiomaActual() {
        LocaleContext contexto = LocaleContextHolder.getLocaleContext();
        Locale pedido = contexto == null ? null : contexto.getLocale();
        return soportado(pedido);
    }

    /** Para los filtros, que corren antes de que Spring fije el idioma de la petición. */
    public Locale idiomaDe(HttpServletRequest peticion) {
        return SelectorDeIdioma.elegir(peticion.getHeader("Accept-Language"));
    }

    public String de(String clave, Object... argumentos) {
        return en(idiomaActual(), clave, argumentos);
    }

    public String en(Locale idioma, String clave, Object... argumentos) {
        return fuente.getMessage(clave, argumentos.length == 0 ? null : argumentos, soportado(idioma));
    }

    private static Locale soportado(Locale idioma) {
        return idioma != null && INGLES.getLanguage().equals(idioma.getLanguage()) ? INGLES : ESPANOL;
    }
}
