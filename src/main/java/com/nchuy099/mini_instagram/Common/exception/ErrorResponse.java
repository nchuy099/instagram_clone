package com.nchuy099.mini_instagram.common.exception;

import java.time.Instant;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {

    private final boolean success = false;
    private int code;
    private String path;
    private String message;
    private Instant timestamp;

    public ErrorResponse(int code, String path, String message) {
        this.code = code;
        this.path = path;
        this.message = message;
        this.timestamp = Instant.now();
    }

}
