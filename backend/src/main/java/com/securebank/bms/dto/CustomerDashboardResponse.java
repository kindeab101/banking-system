package com.securebank.bms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomerDashboardResponse(
        String fullName,
        BigDecimal totalAvailableBalance,
        String currency,
        List<AccountResponse> accounts,
        List<TransactionResponse> recentTransactions,
        List<NotificationResponse> alerts
) {
}
