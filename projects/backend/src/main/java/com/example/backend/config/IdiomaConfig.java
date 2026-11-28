package com.example.backend.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

import com.example.backend.common.idioma.Mensajes;
import com.example.backend.common.idioma.SelectorDeIdioma;

/** Idioma por petición. El validador de Spring Boot usa este mismo MessageSource. */
@Configuration
public class IdiomaConfig {

    @Bean
    public MessageSource messageSource() {
        return Mensajes.crearFuente();
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new SelectorDeIdioma();
    }
}
