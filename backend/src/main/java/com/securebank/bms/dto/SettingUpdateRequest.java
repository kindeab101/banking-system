package com.securebank.bms.dto;

import jakarta.validation.constraints.NotBlank;

public record SettingUpdateRequest(@NotBlank String value) {
}
