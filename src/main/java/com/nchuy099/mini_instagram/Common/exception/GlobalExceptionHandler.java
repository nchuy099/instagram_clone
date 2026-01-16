package com.nchuy099.mini_instagram.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
    // existing code
}