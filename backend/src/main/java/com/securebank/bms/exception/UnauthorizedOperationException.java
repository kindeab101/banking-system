package com.securebank.bms.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedOperationException extends ApiException {
    public UnauthorizedOperationException(String message) {
        super(HttpStatus.FORBIDDEN, "Forbidden", message);
    }
}
