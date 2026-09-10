package com.securebank.bms.dto;

import com.securebank.bms.entity.CustomerStatus;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 30) String phone,
        @Size(max = 255) String addressLine,
        @Size(max = 80) String city,
        CustomerStatus status
) {
}
