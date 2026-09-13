package com.securebank.bms.controller;

import com.securebank.bms.dto.*;
import com.securebank.bms.entity.Role;
import com.securebank.bms.entity.UserStatus;
import com.securebank.bms.security.CurrentUserService;
import com.securebank.bms.service.AuthService;
import com.securebank.bms.service.BankingQueryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminController {

    private final BankingQueryService queryService;
    private final CurrentUserService currentUserService;
    private final AuthService authService;

    public AdminController(BankingQueryService queryService, CurrentUserService currentUserService, AuthService authService) {
        this.queryService = queryService;
        this.currentUserService = currentUserService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return queryService.adminDashboard();
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return queryService.listUsers(page, size);
    }

    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return queryService.createUser(currentUserService.requireUser(), request);
    }

    @PatchMapping("/users/{id}/status")
    public UserResponse status(@PathVariable Long id, @RequestBody UserResponse.StatusUpdate request) {
        return queryService.updateUserStatus(currentUserService.requireUser(), id, request.status() == null ? UserStatus.INACTIVE : request.status());
    }

    @PostMapping("/users/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        authService.adminResetPassword(currentUserService.requireUser(), id, request.newPassword());
        return Map.of("message", "Password reset successfully");
    }

    @PostMapping("/users/{id}/fire")
    public UserResponse fireEmployee(@PathVariable Long id) {
        return queryService.fireEmployee(currentUserService.requireUser(), id);
    }

    @DeleteMapping("/accounts/{accountNumber}")
    public Map<String, String> deleteAccount(@PathVariable String accountNumber) {
        queryService.deleteAccount(currentUserService.requireUser(), accountNumber);
        return Map.of("message", "Account deleted successfully");
    }

    @PatchMapping("/users/{id}/role")
    public UserResponse role(@PathVariable Long id, @RequestBody UserResponse.RoleUpdate request) {
        return queryService.assignRole(currentUserService.requireUser(), id, request.roleCode());
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        return queryService.allRoles().stream().map(this::roleMap).toList();
    }

    @GetMapping("/settings")
    public List<SettingResponse> settings() {
        return queryService.allSettings();
    }

    @PatchMapping("/settings/{key}")
    public SettingResponse updateSetting(@PathVariable String key, @Valid @RequestBody SettingUpdateRequest request) {
        return queryService.updateSetting(currentUserService.requireUser(), key, request.value());
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> audit(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return queryService.searchAudit(q, page, size);
    }

    private Map<String, Object> roleMap(Role role) {
        return Map.of(
                "code", role.getCode(),
                "name", role.getName(),
                "permissions", role.getPermissions() == null ? List.of() : role.getPermissions().stream().map(p -> p.getCode()).sorted().toList()
        );
    }
}
