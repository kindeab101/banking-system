package com.securebank.bms.mapper;

import com.securebank.bms.dto.*;
import com.securebank.bms.entity.*;

import java.util.stream.Collectors;

public final class Mappers {
    private Mappers() {
    }

    public static AccountResponse toAccount(Account account) {
        Customer c = account.getCustomer();
        String name = c.getFirstName() + " " + c.getLastName();
        return new AccountResponse(
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus().name(),
                account.getOpenedAt(),
                c.getCustomerNumber(),
                name
        );
    }

    public static TransactionResponse toTransaction(BankTransaction t) {
        return new TransactionResponse(
                t.getReference(),
                t.getSourceAccount() == null ? null : t.getSourceAccount().getAccountNumber(),
                t.getDestinationAccount() == null ? null : t.getDestinationAccount().getAccountNumber(),
                t.getAmount(),
                t.getCurrency(),
                t.getTransactionType().name(),
                t.getStatus().name(),
                t.getDescription(),
                t.getFailureReason(),
                t.getCreatedAt(),
                t.getCompletedAt()
        );
    }

    public static CustomerResponse toCustomer(Customer c) {
        return toCustomer(c, null);
    }

    public static CustomerResponse toCustomer(Customer c, String primaryAccountNumber) {
        return new CustomerResponse(
                c.getId(),
                c.getCustomerNumber(),
                c.getFirstName(),
                c.getLastName(),
                c.getUser().getEmail(),
                c.getUser().getUsername(),
                c.getPhone(),
                c.getAddressLine(),
                c.getCity(),
                c.getStatus().name(),
                c.getCreatedAt(),
                primaryAccountNumber
        );
    }

    public static UserResponse toUser(UserAccount u) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                u.getStatus().name(),
                u.getRoles().stream().map(Role::getCode).sorted().collect(Collectors.toList())
        );
    }

    public static AuditLogResponse toAudit(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getActor() == null ? null : a.getActor().getUsername(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityReference(),
                a.getResult().name(),
                a.getIpAddress(),
                a.getCreatedAt(),
                a.getMetadata()
        );
    }

    public static NotificationResponse toNotification(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getCreatedAt(), n.getReadAt() != null);
    }

    public static AuthResponse.UserSummary toSummary(UserAccount u) {
        return new AuthResponse.UserSummary(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                u.getStatus().name(),
                u.getRoles().stream().map(Role::getCode).sorted().toList()
        );
    }
}
