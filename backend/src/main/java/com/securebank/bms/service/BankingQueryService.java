package com.securebank.bms.service;

import com.securebank.bms.audit.AuditService;
import com.securebank.bms.dto.*;
import com.securebank.bms.entity.*;
import com.securebank.bms.exception.ResourceNotFoundException;
import com.securebank.bms.exception.UnauthorizedOperationException;
import com.securebank.bms.mapper.Mappers;
import com.securebank.bms.repository.*;
import com.securebank.bms.util.AppUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BankingQueryService {

    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final BankTransactionRepository transactions;
    private final NotificationRepository notifications;
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final AuditLogRepository auditLogs;
    private final SystemSettingRepository settings;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public BankingQueryService(CustomerRepository customers,
                               AccountRepository accounts,
                               BankTransactionRepository transactions,
                               NotificationRepository notifications,
                               UserAccountRepository users,
                               RoleRepository roles,
                               AuditLogRepository auditLogs,
                               SystemSettingRepository settings,
                               PasswordEncoder passwordEncoder,
                               AuditService auditService) {
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
        this.notifications = notifications;
        this.users = users;
        this.roles = roles;
        this.auditLogs = auditLogs;
        this.settings = settings;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public Customer requireCustomer(UserAccount user) {
        return customers.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
    }

    public CustomerDashboardResponse customerDashboard(UserAccount user) {
        Customer customer = requireCustomer(user);
        List<Account> list = accounts.findByCustomerIdOrderByOpenedAtAsc(customer.getId());
        BigDecimal total = list.stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TransactionResponse> recent = list.stream()
                .flatMap(a -> transactions.findForAccount(a.getId(), PageRequest.of(0, 5)).getContent().stream())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(8)
                .map(Mappers::toTransaction)
                .toList();
        var alerts = notifications.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 5))
                .map(Mappers::toNotification)
                .getContent();
        return new CustomerDashboardResponse(
                user.getFullName(),
                total,
                "ETB",
                list.stream().map(Mappers::toAccount).toList(),
                recent,
                alerts
        );
    }

    public ProfileResponse profile(UserAccount user) {
        var customer = customers.findByUserId(user.getId()).orElse(null);
        return new ProfileResponse(
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                customer == null ? null : customer.getCustomerNumber(),
                customer == null ? null : customer.getFirstName(),
                customer == null ? null : customer.getLastName(),
                customer == null ? null : customer.getPhone(),
                customer == null ? null : customer.getDateOfBirth(),
                customer == null ? null : customer.getAddressLine(),
                customer == null ? null : customer.getCity(),
                user.getStatus().name(),
                user.getLastLoginAt()
        );
    }

    @Transactional
    public ProfileResponse updateProfile(UserAccount user, ProfileUpdateRequest request) {
        Customer customer = requireCustomer(user);
        if (request.phone() != null) customer.setPhone(request.phone());
        if (request.addressLine() != null) customer.setAddressLine(request.addressLine());
        if (request.city() != null) customer.setCity(request.city());
        customers.save(customer);
        auditService.record(user, "CUSTOMER_UPDATE", "CUSTOMER", customer.getCustomerNumber(), AuditResult.SUCCESS, "self");
        return profile(user);
    }

    public List<AccountResponse> myAccounts(UserAccount user) {
        Customer customer = requireCustomer(user);
        return accounts.findByCustomerIdOrderByOpenedAtAsc(customer.getId()).stream().map(Mappers::toAccount).toList();
    }

    public Account requireOwnedAccount(UserAccount user, String accountNumber) {
        Account account = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        boolean staff = user.getRoles().stream().anyMatch(r ->
                r.getCode().equals(RoleCode.BANK_EMPLOYEE.name()) || r.getCode().equals(RoleCode.ADMINISTRATOR.name()));
        if (!staff && !account.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("You cannot access this account");
        }
        return account;
    }

    public PageResponse<TransactionResponse> accountTransactions(UserAccount user, String accountNumber, int page, int size) {
        Account account = requireOwnedAccount(user, accountNumber);
        var result = transactions.findForAccount(account.getId(), PageRequest.of(page, Math.min(size, 100)))
                .map(Mappers::toTransaction);
        return AppUtils.page(result);
    }

    public TransactionResponse transactionDetail(UserAccount user, String reference) {
        BankTransaction t = transactions.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        boolean staff = user.getRoles().stream().anyMatch(r ->
                r.getCode().equals(RoleCode.BANK_EMPLOYEE.name()) || r.getCode().equals(RoleCode.ADMINISTRATOR.name()));
        if (!staff) {
            Long uid = user.getId();
            boolean owned = (t.getSourceAccount() != null && t.getSourceAccount().getCustomer().getUser().getId().equals(uid))
                    || (t.getDestinationAccount() != null && t.getDestinationAccount().getCustomer().getUser().getId().equals(uid));
            if (!owned) {
                throw new UnauthorizedOperationException("You cannot access this transaction");
            }
        }
        return Mappers.toTransaction(t);
    }

    public StatementResponse statement(UserAccount user, String accountNumber, LocalDate from, LocalDate to) {
        Account account = requireOwnedAccount(user, accountNumber);
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<BankTransaction> rows = transactions.findForStatement(account.getId(), start, end);
        BigDecimal opening = account.getBalance();
        for (int i = rows.size() - 1; i >= 0; i--) {
            opening = reverseApply(opening, rows.get(i), account);
        }
        BigDecimal running = opening;
        List<StatementResponse.StatementLine> lines = new ArrayList<>();
        for (BankTransaction t : rows) {
            if (t.getStatus() != TransactionStatus.COMPLETED) {
                continue;
            }
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            if (t.getSourceAccount() != null && t.getSourceAccount().getId().equals(account.getId())) {
                debit = t.getAmount();
                running = running.subtract(debit);
            }
            if (t.getDestinationAccount() != null && t.getDestinationAccount().getId().equals(account.getId())) {
                credit = t.getAmount();
                running = running.add(credit);
            }
            lines.add(new StatementResponse.StatementLine(
                    t.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                    t.getReference(),
                    t.getDescription(),
                    debit,
                    credit,
                    running
            ));
        }
        return new StatementResponse(
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName(),
                account.getCurrency(),
                from,
                to,
                opening,
                running,
                lines
        );
    }

    private BigDecimal reverseApply(BigDecimal current, BankTransaction t, Account account) {
        if (t.getStatus() != TransactionStatus.COMPLETED) {
            return current;
        }
        if (t.getSourceAccount() != null && t.getSourceAccount().getId().equals(account.getId())) {
            return current.add(t.getAmount());
        }
        if (t.getDestinationAccount() != null && t.getDestinationAccount().getId().equals(account.getId())) {
            return current.subtract(t.getAmount());
        }
        return current;
    }

    public PageResponse<NotificationResponse> myNotifications(UserAccount user, int page, int size) {
        return AppUtils.page(notifications.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(Mappers::toNotification));
    }

    @Transactional
    public CustomerResponse createCustomer(UserAccount actor, CreateCustomerRequest request) {
        if (users.existsByUsernameIgnoreCase(request.username()) || users.existsByEmailIgnoreCase(request.email())) {
            throw new com.securebank.bms.exception.ApiException(org.springframework.http.HttpStatus.CONFLICT, "Conflict", "Username or email already exists");
        }
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER.name()).orElseThrow();
        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim());
        user.setFullName(request.firstName().trim() + " " + request.lastName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(customerRole);
        users.saveAndFlush(user);
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setCustomerNumber(nextCustomerNumber());
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setPhone(request.phone());
        customer.setAddressLine(request.addressLine());
        customer.setCity(request.city());
        customer.setStatus(CustomerStatus.ACTIVE);
        customers.saveAndFlush(customer);
        Account account = openDefaultSavingsAccount(actor, customer);
        auditService.record(actor, "CUSTOMER_CREATION", "CUSTOMER", customer.getCustomerNumber(), AuditResult.SUCCESS, account.getAccountNumber());
        return Mappers.toCustomer(customer, account.getAccountNumber());
    }

    @Transactional
    public CustomerResponse updateCustomer(UserAccount actor, Long id, UpdateCustomerRequest request) {
        Customer customer = customers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (request.firstName() != null) customer.setFirstName(request.firstName());
        if (request.lastName() != null) customer.setLastName(request.lastName());
        if (request.phone() != null) customer.setPhone(request.phone());
        if (request.addressLine() != null) customer.setAddressLine(request.addressLine());
        if (request.city() != null) customer.setCity(request.city());
        if (request.status() != null) {
            customer.setStatus(request.status());
            if (request.status() == CustomerStatus.INACTIVE) {
                customer.getUser().setStatus(UserStatus.INACTIVE);
            } else {
                customer.getUser().setStatus(UserStatus.ACTIVE);
            }
        }
        customer.getUser().setFullName(customer.getFirstName() + " " + customer.getLastName());
        customers.save(customer);
        auditService.record(actor, "CUSTOMER_UPDATE", "CUSTOMER", customer.getCustomerNumber(), AuditResult.SUCCESS, null);
        return Mappers.toCustomer(customer);
    }

    public PageResponse<CustomerResponse> searchCustomers(String q, CustomerStatus status, int page, int size) {
        return AppUtils.page(customers.search(q, status, PageRequest.of(page, Math.min(size, 50))).map(Mappers::toCustomer));
    }

    public CustomerResponse getCustomer(Long id) {
        return Mappers.toCustomer(customers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found")));
    }

    public List<AccountResponse> customerAccounts(Long customerId) {
        customers.findById(customerId).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return accounts.findByCustomerIdOrderByOpenedAtAsc(customerId).stream().map(Mappers::toAccount).toList();
    }

    @Transactional
    public AccountResponse createAccount(UserAccount actor, CreateAccountRequest request) {
        Customer customer = customers.findByCustomerNumber(request.customerNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Account account = new Account();
        account.setAccountNumber(nextAccountNumber());
        account.setCustomer(customer);
        account.setAccountType(request.accountType());
        account.setCurrency("ETB");
        BigDecimal opening = request.openingBalance() == null ? BigDecimal.ZERO : request.openingBalance();
        if (opening.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.securebank.bms.exception.InvalidTransactionException("Opening balance cannot be negative");
        }
        account.setBalance(opening.setScale(2, java.math.RoundingMode.HALF_UP));
        account.setStatus(AccountStatus.ACTIVE);
        accounts.save(account);
        auditService.record(actor, "ACCOUNT_CREATION", "ACCOUNT", account.getAccountNumber(), AuditResult.SUCCESS, null);
        return Mappers.toAccount(account);
    }

    @Transactional
    public AccountResponse changeAccountStatus(UserAccount actor, String accountNumber, AccountStatus status) {
        Account account = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        account.setStatus(status);
        accounts.save(account);
        Notification n = new Notification();
        n.setUser(account.getCustomer().getUser());
        n.setTitle("Account status updated");
        n.setMessage("Account " + accountNumber + " is now " + status.name());
        notifications.save(n);
        auditService.record(actor, "ACCOUNT_STATUS_CHANGE", "ACCOUNT", accountNumber, AuditResult.SUCCESS, status.name());
        return Mappers.toAccount(account);
    }

    public PageResponse<AccountResponse> searchAccounts(String q, AccountStatus status, int page, int size) {
        return AppUtils.page(accounts.search(q, status, PageRequest.of(page, Math.min(size, 50))).map(Mappers::toAccount));
    }

    public PageResponse<TransactionResponse> searchTransactions(String q, TransactionStatus status, TransactionType type,
                                                               Instant from, Instant to, int page, int size) {
        return AppUtils.page(transactions.search(q, status, type, from, to, PageRequest.of(page, Math.min(size, 100)))
                .map(Mappers::toTransaction));
    }

    public AdminDashboardResponse adminDashboard() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        var recent = auditLogs.search(null, PageRequest.of(0, 8));
        AdminDashboardResponse.ListItem[] items = recent.getContent().stream()
                .map(a -> new AdminDashboardResponse.ListItem(
                        a.getAction(),
                        a.getActor() == null ? "system" : a.getActor().getUsername(),
                        a.getResult().name(),
                        a.getCreatedAt().toString()
                ))
                .toArray(AdminDashboardResponse.ListItem[]::new);
        return new AdminDashboardResponse(
                customers.count(),
                accounts.countByStatus(AccountStatus.ACTIVE),
                transactions.countByCreatedAtGreaterThanEqual(startOfDay),
                transactions.countByStatusAndCreatedAtGreaterThanEqual(TransactionStatus.COMPLETED, startOfDay),
                transactions.countByStatus(TransactionStatus.FAILED),
                items
        );
    }

    public PageResponse<AuditLogResponse> searchAudit(String q, int page, int size) {
        return AppUtils.page(auditLogs.search(q, PageRequest.of(page, Math.min(size, 100))).map(Mappers::toAudit));
    }

    public PageResponse<UserResponse> listUsers(int page, int size) {
        return AppUtils.page(users.findAll(PageRequest.of(page, Math.min(size, 50))).map(Mappers::toUser));
    }

    @Transactional
    public UserResponse createUser(UserAccount actor, CreateUserRequest request) {
        if (users.existsByUsernameIgnoreCase(request.username()) || users.existsByEmailIgnoreCase(request.email())) {
            throw new com.securebank.bms.exception.ApiException(org.springframework.http.HttpStatus.CONFLICT, "Conflict", "Username or email already exists");
        }
        if (RoleCode.CUSTOMER.name().equals(request.roleCode())) {
            String[] names = splitFullName(request.fullName());
            createCustomer(actor, new CreateCustomerRequest(
                    names[0],
                    names[1],
                    request.email(),
                    request.username(),
                    request.temporaryPassword(),
                    null,
                    null,
                    null
            ));
            UserAccount created = users.findByUsernameIgnoreCase(request.username().trim()).orElseThrow();
            return Mappers.toUser(created);
        }
        Role role = roles.findByCode(request.roleCode())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim());
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(role);
        users.saveAndFlush(user);
        auditService.record(actor, "USER_CREATION", "USER", user.getUsername(), AuditResult.SUCCESS, request.roleCode());
        return Mappers.toUser(user);
    }

    @Transactional
    public UserResponse updateUserStatus(UserAccount actor, Long id, UserStatus status) {
        UserAccount user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(status);
        users.save(user);
        auditService.record(actor, "USER_STATUS_CHANGE", "USER", user.getUsername(), AuditResult.SUCCESS, status.name());
        return Mappers.toUser(user);
    }

    @Transactional
    public UserResponse assignRole(UserAccount actor, Long id, String roleCode) {
        UserAccount user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role = roles.findByCode(roleCode).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        user.getRoles().clear();
        user.getRoles().add(role);
        users.saveAndFlush(user);
        if (RoleCode.CUSTOMER.name().equals(roleCode) && customers.findByUserId(user.getId()).isEmpty()) {
            String[] names = splitFullName(user.getFullName());
            Customer customer = new Customer();
            customer.setUser(user);
            customer.setCustomerNumber(nextCustomerNumber());
            customer.setFirstName(names[0]);
            customer.setLastName(names[1]);
            customer.setStatus(CustomerStatus.ACTIVE);
            customers.saveAndFlush(customer);
            openDefaultSavingsAccount(actor, customer);
        }
        auditService.record(actor, "PERMISSION_CHANGE", "USER", user.getUsername(), AuditResult.SUCCESS, roleCode);
        return Mappers.toUser(user);
    }

    public List<Role> allRoles() {
        return roles.findAll();
    }

    public List<SettingResponse> allSettings() {
        return settings.findAll().stream()
                .map(s -> new SettingResponse(s.getKey(), s.getValue(), s.getDescription()))
                .toList();
    }

    @Transactional
    public SettingResponse updateSetting(UserAccount actor, String key, String value) {
        SystemSetting setting = settings.findById(key).orElseThrow(() -> new ResourceNotFoundException("Setting not found"));
        setting.setValue(value);
        settings.save(setting);
        auditService.record(actor, "SETTINGS_CHANGE", "SETTING", key, AuditResult.SUCCESS, null);
        return new SettingResponse(setting.getKey(), setting.getValue(), setting.getDescription());
    }

    private Account openDefaultSavingsAccount(UserAccount actor, Customer customer) {
        Account account = new Account();
        account.setAccountNumber(nextAccountNumber());
        account.setCustomer(customer);
        account.setAccountType(AccountType.SAVINGS);
        account.setCurrency("ETB");
        account.setBalance(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP));
        account.setStatus(AccountStatus.ACTIVE);
        accounts.saveAndFlush(account);
        auditService.record(actor, "ACCOUNT_CREATION", "ACCOUNT", account.getAccountNumber(), AuditResult.SUCCESS, "opened with customer");
        return account;
    }

    private static String[] splitFullName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        if (trimmed.isBlank()) {
            return new String[]{"Customer", "User"};
        }
        String[] parts = trimmed.split("\\s+", 2);
        String first = parts[0];
        String last = parts.length > 1 ? parts[1] : parts[0];
        return new String[]{first, last};
    }

    private String nextCustomerNumber() {
        long count = customers.count() + 1;
        return String.format("CUS-%06d", count);
    }

    private String nextAccountNumber() {
        long count = accounts.count() + 1;
        return String.format("1000%08d", count);
    }
}
