package com.workshop.logproxy.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates failures of the microservices into JSON error responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return respond(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return respond(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Map<String, String>> serverError(RuntimeException e) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    private static ResponseEntity<Map<String, String>> respond(HttpStatus status, RuntimeException e) {
        return ResponseEntity.status(status).body(Map.of("error", status.getReasonPhrase(), "message", String.valueOf(e.getMessage())));
    }
}
