package com.securebank.bms.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends ApiException {
    public InvalidTransactionException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "InvalidTransaction", message);
    }
}
