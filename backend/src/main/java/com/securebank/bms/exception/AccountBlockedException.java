package com.securebank.bms.exception;

import org.springframework.http.HttpStatus;

public class AccountBlockedException extends ApiException {
    public AccountBlockedException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "AccountNotEligible", message);
    }
}
