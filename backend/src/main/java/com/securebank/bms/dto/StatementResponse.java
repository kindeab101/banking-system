package com.securebank.bms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatementResponse(
        String accountNumber,
        String accountType,
        String customerName,
        String currency,
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<StatementLine> lines
) {
    public record StatementLine(
            LocalDate date,
            String reference,
            String description,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal runningBalance
    ) {
    }
}
