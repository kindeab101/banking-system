package com.securebank.bms.service;

import com.securebank.bms.audit.AuditService;
import com.securebank.bms.config.AppProperties;
import com.securebank.bms.dto.AuthResponse;
import com.securebank.bms.dto.ChangePasswordRequest;
import com.securebank.bms.entity.AuditResult;
import com.securebank.bms.entity.RefreshToken;
import com.securebank.bms.entity.Role;
import com.securebank.bms.entity.UserAccount;
import com.securebank.bms.entity.UserStatus;
import com.securebank.bms.exception.ApiException;
import com.securebank.bms.exception.ResourceNotFoundException;
import com.securebank.bms.mapper.Mappers;
import com.securebank.bms.repository.RefreshTokenRepository;
import com.securebank.bms.repository.UserAccountRepository;
import com.securebank.bms.security.JwtService;
import com.securebank.bms.util.AppUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthService {

    private static final String GENERIC_LOGIN_ERROR = "Invalid credentials";

    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final AuditService auditService;

    public AuthService(UserAccountRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AppProperties properties,
                       AuditService auditService) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse login(String usernameOrEmail, String password) {
        UserAccount user = users.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> users.findByEmailIgnoreCase(usernameOrEmail))
                .orElse(null);
        if (user == null) {
            auditService.record(null, "LOGIN_FAILURE", "USER", usernameOrEmail, AuditResult.FAILURE, "unknown user");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", GENERIC_LOGIN_ERROR);
        }
        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            auditService.record(user, "LOGIN_FAILURE", "USER", user.getUsername(), AuditResult.DENIED, "locked");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", GENERIC_LOGIN_ERROR);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            auditService.record(user, "LOGIN_FAILURE", "USER", user.getUsername(), AuditResult.DENIED, "inactive");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", GENERIC_LOGIN_ERROR);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            int max = properties.getSecurity().getMaxFailedLogins();
            if (failures >= max) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(now.plus(properties.getSecurity().getLockoutMinutes(), ChronoUnit.MINUTES));
            }
            users.save(user);
            auditService.record(user, "LOGIN_FAILURE", "USER", user.getUsername(), AuditResult.FAILURE, "bad password");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", GENERIC_LOGIN_ERROR);
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        users.save(user);

        List<String> roles = user.getRoles().stream().map(Role::getCode).toList();
        String access = jwtService.createAccessToken(user.getUsername(), roles);
        String refresh = AppUtils.randomToken();
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setTokenHash(AppUtils.sha256(refresh));
        stored.setExpiresAt(now.plus(properties.getJwt().getRefreshTokenDays(), ChronoUnit.DAYS));
        stored.setRevoked(false);
        refreshTokens.save(stored);
        auditService.record(user, "LOGIN_SUCCESS", "USER", user.getUsername(), AuditResult.SUCCESS, null);
        long expires = properties.getJwt().getAccessTokenMinutes() * 60;
        return new AuthResponse(access, refresh, "Bearer", expires, Mappers.toSummary(user));
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String hash = AppUtils.sha256(refreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Session expired"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Session expired");
        }
        UserAccount user = stored.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Session expired");
        }
        stored.setRevoked(true);
        refreshTokens.save(stored);
        List<String> roles = user.getRoles().stream().map(Role::getCode).toList();
        String access = jwtService.createAccessToken(user.getUsername(), roles);
        String next = AppUtils.randomToken();
        RefreshToken replacement = new RefreshToken();
        replacement.setUser(user);
        replacement.setTokenHash(AppUtils.sha256(next));
        replacement.setExpiresAt(Instant.now().plus(properties.getJwt().getRefreshTokenDays(), ChronoUnit.DAYS));
        refreshTokens.save(replacement);
        long expires = properties.getJwt().getAccessTokenMinutes() * 60;
        return new AuthResponse(access, next, "Bearer", expires, Mappers.toSummary(user));
    }

    @Transactional
    public void logout(UserAccount user, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokens.findByTokenHash(AppUtils.sha256(refreshToken)).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokens.save(token);
            });
        } else {
            refreshTokens.deleteByUserId(user.getId());
        }
        auditService.record(user, "LOGOUT", "USER", user.getUsername(), AuditResult.SUCCESS, null);
    }

    @Transactional
    public void changePassword(UserAccount user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ValidationError", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        refreshTokens.deleteByUserId(user.getId());
        auditService.record(user, "PASSWORD_CHANGE", "USER", user.getUsername(), AuditResult.SUCCESS, null);
    }

    public UserAccount require(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
