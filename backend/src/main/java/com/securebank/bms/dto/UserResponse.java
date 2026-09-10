package com.securebank.bms.dto;

import com.securebank.bms.entity.UserStatus;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String status,
        List<String> roles
) {
    public record StatusUpdate(UserStatus status) {
    }

    public record RoleUpdate(String roleCode) {
    }
}
