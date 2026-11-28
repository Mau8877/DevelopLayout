package com.example.backend.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.backend.common.exception.ValidationException;

/**
 * Valida `page` (1-based) y `pageSize` de todo listado.
 *
 * * `pageSize` tiene máximo: ningún listado se puede pedir sin límite (API4).
 */
public final class Paginacion {

    public static final int PAGE_SIZE_POR_DEFECTO = 20;
    public static final int PAGE_SIZE_MAXIMO = 100;
    private static final int PAGE_MAXIMA = 1_000_000;

    private Paginacion() {
    }

    public static Pageable de(Integer page, Integer pageSize) {
        int pagina = page == null ? 1 : page;
        int tamanio = pageSize == null ? PAGE_SIZE_POR_DEFECTO : pageSize;
        if (pagina < 1 || pagina > PAGE_MAXIMA || tamanio < 1 || tamanio > PAGE_SIZE_MAXIMO) {
            throw new ValidationException("ERR_SYS_01", "common.error.paginacion", PAGE_SIZE_MAXIMO);
        }
        return PageRequest.of(pagina - 1, tamanio);
    }
}
