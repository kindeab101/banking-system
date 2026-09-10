package com.securebank.bms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 4, max = 80) String username,
        @NotBlank @Size(min = 10, max = 128) String temporaryPassword,
        @Size(max = 30) String phone,
        @Size(max = 255) String addressLine,
        @Size(max = 80) String city
) {
}
