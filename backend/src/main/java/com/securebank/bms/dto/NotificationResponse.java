package com.securebank.bms.dto;

import java.time.Instant;

public record NotificationResponse(Long id, String title, String message, Instant createdAt, boolean read) {
}
