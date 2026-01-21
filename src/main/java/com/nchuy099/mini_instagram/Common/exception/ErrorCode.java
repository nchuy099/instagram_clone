package com.nchuy099.mini_instagram.common.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    BAD_REQUEST(400, "Bad Request", HttpStatus.BAD_REQUEST),
    NOT_FOUND(404, "Not Found", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    CONFLICT(409, "Conflict", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus status;

}
