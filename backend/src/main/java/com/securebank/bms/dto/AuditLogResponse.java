package com.securebank.bms.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actorUsername,
        String action,
        String entityType,
        String entityReference,
        String result,
        String ipAddress,
        Instant createdAt,
        String metadata
) {
}
