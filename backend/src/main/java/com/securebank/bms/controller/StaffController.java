package com.securebank.bms.controller;

import com.securebank.bms.dto.*;
import com.securebank.bms.entity.AccountStatus;
import com.securebank.bms.entity.CustomerStatus;
import com.securebank.bms.entity.TransactionStatus;
import com.securebank.bms.entity.TransactionType;
import com.securebank.bms.security.CurrentUserService;
import com.securebank.bms.service.BankingQueryService;
import com.securebank.bms.service.ExportService;
import com.securebank.bms.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('BANK_EMPLOYEE','ADMINISTRATOR')")
public class StaffController {

    private final BankingQueryService queryService;
    private final TransferService transferService;
    private final CurrentUserService currentUserService;
    private final ExportService exportService;

    public StaffController(BankingQueryService queryService,
                           TransferService transferService,
                           CurrentUserService currentUserService,
                           ExportService exportService) {
        this.queryService = queryService;
        this.transferService = transferService;
        this.currentUserService = currentUserService;
        this.exportService = exportService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return queryService.adminDashboard();
    }

    @GetMapping("/customers")
    public PageResponse<CustomerResponse> customers(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) CustomerStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return queryService.searchCustomers(q, status, page, size);
    }

    @PostMapping("/customers")
    public CustomerResponse createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return queryService.createCustomer(currentUserService.requireUser(), request);
    }

    @GetMapping("/customers/{id}")
    public CustomerResponse customer(@PathVariable Long id) {
        return queryService.getCustomer(id);
    }

    @PatchMapping("/customers/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {
        return queryService.updateCustomer(currentUserService.requireUser(), id, request);
    }

    @GetMapping("/customers/{id}/accounts")
    public List<AccountResponse> customerAccounts(@PathVariable Long id) {
        return queryService.customerAccounts(id);
    }

    @GetMapping("/accounts")
    public PageResponse<AccountResponse> accounts(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) AccountStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return queryService.searchAccounts(q, status, page, size);
    }

    @PostMapping("/accounts")
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return queryService.createAccount(currentUserService.requireUser(), request);
    }

    @PatchMapping("/accounts/{accountNumber}/status")
    public AccountResponse status(@PathVariable String accountNumber, @Valid @RequestBody AccountStatusRequest request) {
        return queryService.changeAccountStatus(currentUserService.requireUser(), accountNumber, request.status());
    }

    @GetMapping("/transactions")
    public PageResponse<TransactionResponse> transactions(@RequestParam(required = false) String q,
                                                          @RequestParam(required = false) TransactionStatus status,
                                                          @RequestParam(required = false) TransactionType type,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        Instant fromTs = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTs = to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return queryService.searchTransactions(q, status, type, fromTs, toTs, page, size);
    }

    @PostMapping("/transactions/deposit")
    public TransferResponse deposit(@Valid @RequestBody StaffPostingRequest request) {
        return transferService.deposit(currentUserService.requireUser(), request);
    }

    @PostMapping("/transactions/withdrawal")
    public TransferResponse withdrawal(@Valid @RequestBody StaffPostingRequest request) {
        return transferService.withdraw(currentUserService.requireUser(), request);
    }

    @GetMapping("/reports/transactions.csv")
    public ResponseEntity<byte[]> csv(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) TransactionStatus status,
                                      @RequestParam(required = false) TransactionType type) {
        var page = queryService.searchTransactions(q, status, type, null, null, 0, 500);
        byte[] csv = exportService.transactionsCsv(page.content());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions-demo.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
