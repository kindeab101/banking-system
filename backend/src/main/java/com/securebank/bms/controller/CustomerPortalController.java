package com.securebank.bms.controller;

import com.securebank.bms.dto.*;
import com.securebank.bms.entity.UserAccount;
import com.securebank.bms.security.CurrentUserService;
import com.securebank.bms.service.BankingQueryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CustomerPortalController {

    private final CurrentUserService currentUserService;
    private final BankingQueryService queryService;

    public CustomerPortalController(CurrentUserService currentUserService, BankingQueryService queryService) {
        this.currentUserService = currentUserService;
        this.queryService = queryService;
    }

    @GetMapping("/me")
    public ProfileResponse me() {
        return queryService.profile(currentUserService.requireUser());
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ProfileResponse updateMe(@Valid @RequestBody ProfileUpdateRequest request) {
        return queryService.updateProfile(currentUserService.requireUser(), request);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerDashboardResponse dashboard() {
        return queryService.customerDashboard(currentUserService.requireUser());
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<AccountResponse> accounts() {
        return queryService.myAccounts(currentUserService.requireUser());
    }

    @GetMapping("/accounts/{accountNumber}")
    public AccountResponse account(@PathVariable String accountNumber) {
        UserAccount user = currentUserService.requireUser();
        return com.securebank.bms.mapper.Mappers.toAccount(queryService.requireOwnedAccount(user, accountNumber));
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    public PageResponse<TransactionResponse> accountTransactions(@PathVariable String accountNumber,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return queryService.accountTransactions(currentUserService.requireUser(), accountNumber, page, size);
    }

    @GetMapping("/transactions/{reference}")
    public TransactionResponse transaction(@PathVariable String reference) {
        return queryService.transactionDetail(currentUserService.requireUser(), reference);
    }

    @GetMapping("/notifications")
    public PageResponse<NotificationResponse> notifications(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return queryService.myNotifications(currentUserService.requireUser(), page, size);
    }
}
