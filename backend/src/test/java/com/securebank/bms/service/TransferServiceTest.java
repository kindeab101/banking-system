package com.securebank.bms.service;

import com.securebank.bms.audit.AuditService;
import com.securebank.bms.config.AppProperties;
import com.securebank.bms.dto.TransferRequest;
import com.securebank.bms.entity.*;
import com.securebank.bms.exception.AccountBlockedException;
import com.securebank.bms.exception.InsufficientBalanceException;
import com.securebank.bms.exception.InvalidTransactionException;
import com.securebank.bms.exception.ResourceNotFoundException;
import com.securebank.bms.repository.AccountRepository;
import com.securebank.bms.repository.BankTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountRepository accounts;
    @Mock BankTransactionRepository transactions;
    @Mock AuditService auditService;
    @Mock NotificationService notifications;
    AppProperties properties = new AppProperties();
    TransferService service;

    UserAccount owner;
    Account source;
    Account dest;

    @BeforeEach
    void setUp() {
        service = new TransferService(accounts, transactions, auditService, notifications, properties);
        owner = new UserAccount();
        owner.setId(1L);
        owner.setUsername("customer.a");
        Customer c1 = new Customer();
        c1.setUser(owner);
        Customer c2 = new Customer();
        UserAccount other = new UserAccount();
        other.setId(2L);
        c2.setUser(other);
        source = account(10L, "1000000001", c1, "50000.00", AccountStatus.ACTIVE);
        dest = account(11L, "1000000003", c2, "20000.00", AccountStatus.ACTIVE);
    }

    @Test
    void successfulTransferUpdatesBothBalances() {
        stubLookupAndLock();
        when(transactions.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        var response = service.transfer(owner, new TransferRequest("1000000001", "1000000003", new BigDecimal("5000.00"), "demo"), "key-1", false);
        assertEquals("COMPLETED", response.status());
        assertEquals(new BigDecimal("45000.00"), source.getBalance());
        assertEquals(new BigDecimal("25000.00"), dest.getBalance());
    }

    @Test
    void insufficientFundsIsRejected() {
        stubLookupAndLock();
        assertThrows(InsufficientBalanceException.class, () ->
                service.transfer(owner, new TransferRequest("1000000001", "1000000003", new BigDecimal("99999.00"), "x"), null, false));
        assertEquals(new BigDecimal("50000.00"), source.getBalance());
    }

    @Test
    void negativeAmountRejectedByValidationLayerEquivalent() {
        assertThrows(InvalidTransactionException.class, () ->
                service.transfer(owner, new TransferRequest("1000000001", "1000000003", new BigDecimal("-1.00"), "x"), null, false));
    }

    @Test
    void missingDestinationRejected() {
        when(accounts.findByAccountNumber("1000000001")).thenReturn(Optional.of(source));
        when(accounts.findByAccountNumber("999")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.transfer(owner, new TransferRequest("1000000001", "999", new BigDecimal("10.00"), "x"), null, false));
    }

    @Test
    void blockedSourceRejected() {
        source.setStatus(AccountStatus.BLOCKED);
        stubLookupAndLock();
        assertThrows(AccountBlockedException.class, () ->
                service.transfer(owner, new TransferRequest("1000000001", "1000000003", new BigDecimal("10.00"), "x"), null, false));
    }

    @Test
    void idempotentReplayDoesNotTransferTwice() {
        BankTransaction existing = new BankTransaction();
        existing.setReference("TXN-20260909-AAAAAAAA");
        existing.setStatus(TransactionStatus.COMPLETED);
        existing.setAmount(new BigDecimal("5000.00"));
        existing.setCurrency("ETB");
        existing.setSourceAccount(source);
        existing.setDestinationAccount(dest);
        existing.setCreatedAt(java.time.Instant.now());
        when(transactions.findByCreatedByIdAndIdempotencyKey(1L, "same")).thenReturn(Optional.of(existing));
        var first = service.transfer(owner, new TransferRequest("1000000001", "1000000003", new BigDecimal("5000.00"), "x"), "same", false);
        assertEquals("TXN-20260909-AAAAAAAA", first.transactionReference());
        verify(accounts, never()).findByIdForUpdate(any());
    }

    private void stubLookupAndLock() {
        when(accounts.findByAccountNumber("1000000001")).thenReturn(Optional.of(source));
        when(accounts.findByAccountNumber("1000000003")).thenReturn(Optional.of(dest));
        when(accounts.findByIdForUpdate(10L)).thenReturn(Optional.of(source));
        when(accounts.findByIdForUpdate(11L)).thenReturn(Optional.of(dest));
    }

    private Account account(Long id, String number, Customer customer, String balance, AccountStatus status) {
        Account a = new Account();
        a.setId(id);
        a.setAccountNumber(number);
        a.setCustomer(customer);
        a.setBalance(new BigDecimal(balance));
        a.setStatus(status);
        a.setCurrency("ETB");
        a.setAccountType(AccountType.SAVINGS);
        return a;
    }
}
