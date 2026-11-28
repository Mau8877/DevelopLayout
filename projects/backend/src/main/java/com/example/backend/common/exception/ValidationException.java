package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BusinessException {

    public ValidationException(String errorCode, String clave, Object... argumentos) {
        super(errorCode, HttpStatus.BAD_REQUEST, clave, argumentos);
    }
}
