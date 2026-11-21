package com.gateprep.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Validation errors use the same JSON shape as the other errors: {status, error, message}. */
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().findFirst()
            .map(f -> f.getField() + " " + f.getDefaultMessage()).orElse("Invalid request");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", msg);
        return ResponseEntity.badRequest().body(body);
    }
}
