package com.securebank.bms.controller;

import com.securebank.bms.dto.AuthResponse;
import com.securebank.bms.dto.ChangePasswordRequest;
import com.securebank.bms.dto.LoginRequest;
import com.securebank.bms.dto.RefreshRequest;
import com.securebank.bms.security.CurrentUserService;
import com.securebank.bms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        String token = request == null ? null : request.refreshToken();
        authService.logout(currentUserService.requireUser(), token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserService.requireUser(), request);
        return ResponseEntity.noContent().build();
    }
}
