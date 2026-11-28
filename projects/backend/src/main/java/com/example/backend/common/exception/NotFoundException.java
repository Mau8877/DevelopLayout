package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String errorCode, String clave, Object... argumentos) {
        super(errorCode, HttpStatus.NOT_FOUND, clave, argumentos);
    }
}
