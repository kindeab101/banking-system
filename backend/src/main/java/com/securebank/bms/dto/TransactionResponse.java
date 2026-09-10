package com.securebank.bms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String reference,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String transactionType,
        String status,
        String description,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
}
