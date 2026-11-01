package com.tourmate.exception;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(), Objects.requireNonNullElse(ex.getReason(), "Request failed")));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Please check your input");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        problem.setProperty("errors", errors); return ResponseEntity.badRequest().body(problem);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A record with these details already exists"));
    }
}
