package com.zym.hd.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("bad request: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("unauthorized") || message.contains("session expired") || message.contains("token")) {
            log.warn("unauthorized: {}", ex.getMessage(), ex);
            return build(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        log.warn("conflict/state error: {}", ex.getMessage(), ex);
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("forbidden: {}", ex.getMessage(), ex);
        return build(HttpStatus.FORBIDDEN, "forbidden");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateKey(DuplicateKeyException ex) {
        String message = "duplicate data";
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("phone")) {
            message = "phone already exists";
        }
        log.warn("duplicate key: {}", ex.getMessage(), ex);
        return build(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadParams(Exception ex) {
        log.warn("invalid request params: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, "invalid request parameters");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("internal server error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

