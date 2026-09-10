package com.securebank.bms.dto;

import com.securebank.bms.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull AccountStatus status) {
}
