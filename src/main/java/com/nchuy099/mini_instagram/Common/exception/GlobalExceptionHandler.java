package com.nchuy099.mini_instagram.common.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Business exception
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest req) {
        ErrorCode errorCode = ex.getErrorCode();
        String path = req.getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getCode(),
                path,
                ex.getMessage());
        return new ResponseEntity<>(errorResponse, errorCode.getStatus());

    }

    // @RequestBody validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
            HttpServletRequest req) {

        String message = ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, req.getRequestURI(), message));
    }

    // @RequestParam / @PathVariable validation
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        String message = ex.getConstraintViolations()
                .iterator()
                .next()
                .getMessage();

        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, request.getRequestURI(), message));
    }

    // Missing request param
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        String message = "Missing parameter: " + ex.getParameterName();

        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, request.getRequestURI(), message));
    }

    // Invalid JSON / parse error
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, request.getRequestURI(), "Invalid request body"));
    }

    // Param type mismatchs
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String message = String.format(
                "Invalid value for '%s'", ex.getName());

        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, request.getRequestURI(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest req) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        String path = req.getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getCode(),
                path,
                "An unexpected error occurred.");
        return new ResponseEntity<>(errorResponse, errorCode.getStatus());
    }
}