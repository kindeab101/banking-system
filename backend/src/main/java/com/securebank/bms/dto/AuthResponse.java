package com.securebank.bms.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserSummary user
) {
    public record UserSummary(
            Long id,
            String username,
            String email,
            String fullName,
            String status,
            List<String> roles
    ) {
    }
}
