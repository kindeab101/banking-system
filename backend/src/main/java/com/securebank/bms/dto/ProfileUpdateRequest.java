package com.securebank.bms.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 30) @Pattern(regexp = "^[0-9+\\-\\s]*$") String phone,
        @Size(max = 255) String addressLine,
        @Size(max = 80) String city
) {
}
