package com.securebank.bms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProfileResponse(
        String username,
        String email,
        String fullName,
        String customerNumber,
        String firstName,
        String lastName,
        String phone,
        LocalDate dateOfBirth,
        String addressLine,
        String city,
        String status,
        Instant lastLoginAt
) {
}
