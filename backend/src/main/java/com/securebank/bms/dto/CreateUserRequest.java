package com.securebank.bms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Size(min = 10, max = 128) String temporaryPassword,
        @NotBlank String roleCode
) {
}
