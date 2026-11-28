package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    private final transient Object[] argumentos;

    /** `clave` es la del mensaje en resources/i18n; `getMessage()` la devuelve (útil en logs). */
    protected BusinessException(String errorCode, HttpStatus httpStatus, String clave, Object... argumentos) {
        super(clave);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.argumentos = argumentos.clone();
    }

    public Object[] getArgumentos() {
        return argumentos == null ? new Object[0] : argumentos.clone();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
