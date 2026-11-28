package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

    public ConflictException(String errorCode, String clave, Object... argumentos) {
        super(errorCode, HttpStatus.CONFLICT, clave, argumentos);
    }
}
