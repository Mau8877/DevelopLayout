package com.example.backend.common.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Rechaza las contraseñas muy usadas.
 *
 * ! Solo para una contraseña NUEVA. Nunca en el inicio de sesión ni en la
 * ! contraseña actual: dejaría fuera a quien ya la tiene.
 */
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = PasswordNoComun.Validador.class)
public @interface PasswordNoComun {

    String message() default "{common.validacion.passwordComun}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<PasswordNoComun, String> {

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            return valor == null || !ContrasenasComunes.contiene(valor);
        }
    }
}
