package org.ecom.productcatalog.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, request, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String paramName = ex.getName() != null ? ex.getName() : "parameter";
        String message = String.format("%s must be a valid number", paramName);
        return buildError(HttpStatus.BAD_REQUEST, request, message);
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status, HttpServletRequest request, String message) {
        ApiError error = new ApiError(status.value(), request.getRequestURI(), message);
        return ResponseEntity.status(status).body(error);
    }
}

