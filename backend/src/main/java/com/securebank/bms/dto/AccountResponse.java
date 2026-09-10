package com.securebank.bms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String accountNumber,
        String accountType,
        String currency,
        BigDecimal balance,
        String status,
        Instant openedAt,
        String customerNumber,
        String customerName
) {
}
