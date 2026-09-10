package com.securebank.bms.service;

import com.securebank.bms.audit.AuditService;
import com.securebank.bms.config.AppProperties;
import com.securebank.bms.dto.StaffPostingRequest;
import com.securebank.bms.dto.TransferRequest;
import com.securebank.bms.dto.TransferResponse;
import com.securebank.bms.entity.*;
import com.securebank.bms.exception.*;
import com.securebank.bms.repository.AccountRepository;
import com.securebank.bms.repository.BankTransactionRepository;
import com.securebank.bms.util.AppUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final BankTransactionRepository transactions;
    private final AuditService auditService;
    private final NotificationService notifications;
    private final AppProperties properties;

    public TransferService(AccountRepository accounts,
                           BankTransactionRepository transactions,
                           AuditService auditService,
                           NotificationService notifications,
                           AppProperties properties) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.auditService = auditService;
        this.notifications = notifications;
        this.properties = properties;
    }

    @Transactional
    public TransferResponse transfer(UserAccount actor, TransferRequest request, String idempotencyKey, boolean staffOverride) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = transactions.findByCreatedByIdAndIdempotencyKey(actor.getId(), idempotencyKey);
            if (existing.isPresent()) {
                BankTransaction t = existing.get();
                return toResponse(t, "Idempotent replay of an existing transfer");
            }
        }

        BigDecimal amount = scaleMoney(request.amount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }
        if (amount.compareTo(properties.getTransfer().getMaxAmount()) > 0) {
            throw new InvalidTransactionException("Amount exceeds the maximum allowed transfer");
        }
        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            throw new InvalidTransactionException("Source and destination accounts must be different");
        }

        Account sourceLookup = accounts.findByAccountNumber(request.sourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account destLookup = accounts.findByAccountNumber(request.destinationAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (!staffOverride && !sourceLookup.getCustomer().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("You can only transfer from your own accounts");
        }

        Long firstId = Math.min(sourceLookup.getId(), destLookup.getId());
        Long secondId = Math.max(sourceLookup.getId(), destLookup.getId());
        Account first = accounts.findByIdForUpdate(firstId).orElseThrow();
        Account second = accounts.findByIdForUpdate(secondId).orElseThrow();
        Account source = first.getId().equals(sourceLookup.getId()) ? first : second;
        Account destination = first.getId().equals(destLookup.getId()) ? first : second;

        try {
            assertActive(source, "Source account is not eligible for transfers");
            assertActive(destination, "Destination account is not eligible for transfers");
            if (!"ETB".equals(source.getCurrency()) || !source.getCurrency().equals(destination.getCurrency())) {
                throw new InvalidTransactionException("Currency mismatch");
            }
            if (source.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException();
            }

            source.setBalance(source.getBalance().subtract(amount));
            destination.setBalance(destination.getBalance().add(amount));
            accounts.save(source);
            accounts.save(destination);

            BankTransaction txn = persist(TransactionType.TRANSFER, TransactionStatus.COMPLETED, amount,
                    source, destination, request.description(), null, actor, idempotencyKey);
            auditService.record(actor, "TRANSFER_COMPLETION", "TRANSACTION", txn.getReference(), AuditResult.SUCCESS, amount.toPlainString());
            notifications.notify(source.getCustomer().getUser(), "Transfer completed",
                    "Debit of " + amount.toPlainString() + " ETB. Ref " + txn.getReference());
            notifications.notify(destination.getCustomer().getUser(), "Incoming transfer",
                    "Credit of " + amount.toPlainString() + " ETB. Ref " + txn.getReference());
            return toResponse(txn, "Transfer completed");
        } catch (RuntimeException ex) {
            auditService.record(actor, "TRANSFER_FAILURE", "TRANSACTION",
                    request.sourceAccountNumber(), AuditResult.FAILURE, safeMessage(ex));
            throw ex;
        }
    }

    @Transactional
    public TransferResponse deposit(UserAccount actor, StaffPostingRequest request) {
        Account account = lockOne(request.accountNumber());
        assertActive(account, "Account is not eligible for deposits");
        BigDecimal amount = scaleMoney(request.amount());
        account.setBalance(account.getBalance().add(amount));
        accounts.save(account);
        BankTransaction txn = persist(TransactionType.DEPOSIT_SIMULATION, TransactionStatus.COMPLETED, amount,
                null, account, request.description(), null, actor, null);
        auditService.record(actor, "DEPOSIT_SIMULATION", "TRANSACTION", txn.getReference(), AuditResult.SUCCESS, amount.toPlainString());
        notifications.notify(account.getCustomer().getUser(), "Deposit posted", amount.toPlainString() + " ETB credited. Ref " + txn.getReference());
        return toResponse(txn, "Simulated deposit completed");
    }

    @Transactional
    public TransferResponse withdraw(UserAccount actor, StaffPostingRequest request) {
        Account account = lockOne(request.accountNumber());
        assertActive(account, "Account is not eligible for withdrawals");
        BigDecimal amount = scaleMoney(request.amount());
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }
        account.setBalance(account.getBalance().subtract(amount));
        accounts.save(account);
        BankTransaction txn = persist(TransactionType.WITHDRAWAL_SIMULATION, TransactionStatus.COMPLETED, amount,
                account, null, request.description(), null, actor, null);
        auditService.record(actor, "WITHDRAWAL_SIMULATION", "TRANSACTION", txn.getReference(), AuditResult.SUCCESS, amount.toPlainString());
        notifications.notify(account.getCustomer().getUser(), "Withdrawal posted", amount.toPlainString() + " ETB debited. Ref " + txn.getReference());
        return toResponse(txn, "Simulated withdrawal completed");
    }

    private Account lockOne(String accountNumber) {
        Account found = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return accounts.findByIdForUpdate(found.getId()).orElseThrow();
    }

    private void assertActive(Account account, String message) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBlockedException(message);
        }
    }

    private BankTransaction persist(TransactionType type, TransactionStatus status, BigDecimal amount,
                                    Account source, Account dest, String description, String failure,
                                    UserAccount actor, String idempotencyKey) {
        BankTransaction txn = new BankTransaction();
        String reference = AppUtils.transactionReference();
        while (transactions.existsByReference(reference)) {
            reference = AppUtils.transactionReference();
        }
        txn.setReference(reference);
        txn.setSourceAccount(source);
        txn.setDestinationAccount(dest);
        txn.setAmount(amount);
        txn.setCurrency("ETB");
        txn.setTransactionType(type);
        txn.setStatus(status);
        txn.setDescription(description);
        txn.setFailureReason(failure);
        txn.setCreatedBy(actor);
        txn.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey);
        if (status == TransactionStatus.COMPLETED) {
            txn.setCompletedAt(Instant.now());
        }
        return transactions.save(txn);
    }

    private TransferResponse toResponse(BankTransaction t, String message) {
        return new TransferResponse(
                t.getReference(),
                t.getStatus().name(),
                t.getAmount(),
                t.getCurrency(),
                t.getSourceAccount() == null ? null : t.getSourceAccount().getAccountNumber(),
                t.getDestinationAccount() == null ? null : t.getDestinationAccount().getAccountNumber(),
                t.getCreatedAt(),
                message
        );
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidTransactionException("Amount must have at most two decimal places");
        }
    }

    private String safeMessage(Exception ex) {
        String m = ex.getMessage();
        return m == null ? ex.getClass().getSimpleName() : m.substring(0, Math.min(m.length(), 200));
    }
}
