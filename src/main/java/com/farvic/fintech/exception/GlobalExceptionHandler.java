package com.farvic.fintech.exception;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setType(URI.create("https://api.fintech/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Business rule violation");
        problem.setType(URI.create("https://api.fintech/errors/business-rule"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ProblemDetail handleUnauthorizedOp(UnauthorizedOperationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Unauthorized operation");
        problem.setType(URI.create("https://api.fintech/errors/forbidden-operation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation error");
        problem.setType(URI.create("https://api.fintech/errors/validation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("errors", fieldErrors);

        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Constraint violation");
        problem.setType(URI.create("https://api.fintech/errors/constraint-violation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        problem.setTitle("Access denied");
        problem.setType(URI.create("https://api.fintech/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setType(URI.create("https://api.fintech/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problem.setTitle("Internal server error");
        problem.setType(URI.create("https://api.fintech/errors/internal"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }
}