package com.securebank.bms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank @Size(min = 8, max = 20) String sourceAccountNumber,
        @NotBlank @Size(min = 8, max = 20) String destinationAccountNumber,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @Size(max = 255) String description
) {
}
