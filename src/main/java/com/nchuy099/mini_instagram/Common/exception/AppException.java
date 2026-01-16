package com.nchuy099.mini_instagram.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
