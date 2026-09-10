package com.securebank.bms.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends ApiException {
    public InsufficientBalanceException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "InsufficientBalance", "Insufficient available balance for this transfer");
    }
}
