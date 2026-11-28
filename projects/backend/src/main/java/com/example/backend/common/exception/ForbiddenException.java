package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String errorCode, String clave, Object... argumentos) {
        super(errorCode, HttpStatus.FORBIDDEN, clave, argumentos);
    }
}
