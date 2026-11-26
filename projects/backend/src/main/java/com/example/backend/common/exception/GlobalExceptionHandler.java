package com.example.backend.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.TypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.backend.common.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Los datos enviados no son válidos");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("ERR_SYS_01", message));
    }

    // * Body ilegible: JSON mal formado o con propiedades desconocidas (mass assignment).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("ERR_SYS_01", "El cuerpo de la solicitud no es válido"));
    }

    // * Los rechazos de @PreAuthorize se relanzan: Spring Security los convierte
    // * en 401/403 (RestAuthenticationEntryPoint / RestAccessDeniedHandler).
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void handleSecurityException(RuntimeException ex) {
        throw ex;
    }

    // * Parámetro de ruta o de query que no se puede convertir al tipo esperado.
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(TypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("ERR_SYS_01", "Los datos enviados no son válidos"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpectedException(Exception ex) {
        // * Las excepciones de Spring MVC (ruta, método, parámetros...) ya traen su
        // * status: un error del cliente no es un 500 ni se loguea como ERROR.
        if (ex instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            return errorDelCliente(error);
        }
        LOG.error("Error interno no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("ERR_SYS_00", "Ocurrió un error inesperado"));
    }

    private static ResponseEntity<ApiResponse<Object>> errorDelCliente(ErrorResponse error) {
        HttpStatusCode status = error.getStatusCode();
        ApiResponse<Object> body = switch (status.value()) {
            case 400 -> ApiResponse.error("ERR_SYS_01", "Los datos enviados no son válidos");
            case 404 -> ApiResponse.error("ERR_SYS_03", "El recurso solicitado no existe");
            case 405 -> ApiResponse.error("ERR_SYS_04", "Método no permitido");
            default -> ApiResponse.error("ERR_SYS_05", "La solicitud no es compatible con este recurso");
        };
        // * Conserva las cabeceras propias del error (ej. `Allow` en el 405).
        return ResponseEntity.status(status).headers(error.getHeaders()).body(body);
    }
}
