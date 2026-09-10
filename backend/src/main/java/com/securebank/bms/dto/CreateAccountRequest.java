package com.securebank.bms.dto;

import com.securebank.bms.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String customerNumber,
        @NotNull AccountType accountType,
        BigDecimal openingBalance
) {
}
