package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String errorCode, String clave, Object... argumentos) {
        super(errorCode, HttpStatus.UNAUTHORIZED, clave, argumentos);
    }
}
