package com.example.backend.common.exception;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.hibernate.exception.ConstraintViolationException;

/**
 * Copia de una excepción para el log: misma traza y mismas causas, sin sus
 * mensajes.
 *
 * * Los mensajes de la base y de las librerías repiten los datos que fallaron
 * * (un correo repetido, un valor mal formado). Se conserva lo que permite
 * * diagnosticar: la clase, dónde ocurrió, el SQLState y la restricción.
 * ! Toda excepción que no sea nuestra se loguea a través de esta clase.
 */
public final class ExcepcionSinMensajes extends RuntimeException {

    private ExcepcionSinMensajes(String descripcion, Throwable causa) {
        super(descripcion, causa);
    }

    public static ExcepcionSinMensajes de(Throwable original) {
        return copiar(original, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static ExcepcionSinMensajes copiar(Throwable original, Set<Throwable> vistas) {
        vistas.add(original);
        Throwable causa = original.getCause();
        ExcepcionSinMensajes copia = new ExcepcionSinMensajes(describir(original),
                causa == null || vistas.contains(causa) ? null : copiar(causa, vistas));
        copia.setStackTrace(original.getStackTrace());
        return copia;
    }

    private static String describir(Throwable error) {
        StringBuilder descripcion = new StringBuilder(error.getClass().getName());
        if (error instanceof SQLException sql && sql.getSQLState() != null) {
            descripcion.append(" sqlState=").append(sql.getSQLState());
        }
        // * El nombre de la restricción lo pone una migración: no es un dato de nadie.
        if (error instanceof ConstraintViolationException violacion && violacion.getConstraintName() != null) {
            descripcion.append(" restriccion=").append(violacion.getConstraintName());
        }
        return descripcion.toString();
    }
}
