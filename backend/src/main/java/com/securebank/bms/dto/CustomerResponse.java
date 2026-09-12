package com.securebank.bms.dto;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String customerNumber,
        String firstName,
        String lastName,
        String email,
        String username,
        String phone,
        String addressLine,
        String city,
        String status,
        Instant createdAt,
        String primaryAccountNumber
) {
}
