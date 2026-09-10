package com.securebank.bms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String transactionReference,
        String status,
        BigDecimal amount,
        String currency,
        String sourceAccountNumber,
        String destinationAccountNumber,
        Instant timestamp,
        String message
) {
}
