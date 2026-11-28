package com.example.backend.features.auth.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Máximo en BYTES (UTF-8), no en caracteres.
 *
 * * BCrypt solo admite 72 bytes: 40 letras con tilde son 80 bytes y al
 * * codificarlas daría un error 500 en vez de un 400.
 */
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = MaximoBytes.Validador.class)
public @interface MaximoBytes {

    int value();

    String message() default "{auth.validacion.passwordMaximo}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<MaximoBytes, String> {

        private int maximo;

        @Override
        public void initialize(MaximoBytes anotacion) {
            this.maximo = anotacion.value();
        }

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            return valor == null || valor.getBytes(StandardCharsets.UTF_8).length <= maximo;
        }
    }
}
