package com.causal.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of(
                        "code", "invalid_request",
                        "message", fe.getDefaultMessage(),
                        "details", Map.of("field", fe.getField())
                ))
                .toList();
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("errors", List.of(Map.of(
                        "code", "request_error",
                        "message", ex.getReason() != null ? ex.getReason() : "Request failed"
                ))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(Map.of("errors", List.of(Map.of(
                            "code", "invalid_request",
                            "message", errorResponse.getBody().getDetail() != null
                                    ? errorResponse.getBody().getDetail() : "Bad request"
                    ))));
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(Map.of("errors", List.of(Map.of(
                        "code", "internal_error",
                        "message", "An unexpected error occurred"
                ))));
    }
}
