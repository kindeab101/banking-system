package com.securebank.bms.seed;

import com.securebank.bms.config.AppProperties;
import com.securebank.bms.entity.*;
import com.securebank.bms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProperties properties;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserAccountRepository users;
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final BankTransactionRepository transactions;
    private final SystemSettingRepository settings;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppProperties properties,
                      RoleRepository roles,
                      PermissionRepository permissions,
                      UserAccountRepository users,
                      CustomerRepository customers,
                      AccountRepository accounts,
                      BankTransactionRepository transactions,
                      SystemSettingRepository settings,
                      PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.roles = roles;
        this.permissions = permissions;
        this.users = users;
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
        this.settings = settings;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.getDemo().isSeed()) {
            return;
        }
        ensureCatalog();
        if (users.findByUsernameIgnoreCase("admin").isPresent()) {
            return;
        }
        log.info("Seeding DEMO / TEST users and accounts (fictional data only)");
        Role adminRole = roles.findByCode("ADMINISTRATOR").orElseThrow();
        Role staffRole = roles.findByCode("BANK_EMPLOYEE").orElseThrow();
        Role customerRole = roles.findByCode("CUSTOMER").orElseThrow();

        UserAccount admin = user("admin", "admin@securebank.demo", "System Administrator", "DemoAdmin#2026", adminRole);
        user("employee", "employee@securebank.demo", "Hana Bekele", "DemoStaff#2026", staffRole);

        UserAccount customerAUser = user("customer.a", "abebe.kebede@securebank.demo", "Abebe Kebede", "DemoCustomer#2026", customerRole);
        UserAccount customerBUser = user("customer.b", "sara.tesfaye@securebank.demo", "Sara Tesfaye", "DemoCustomer#2026", customerRole);

        Customer a = customer(customerAUser, "CUS-000001", "Abebe", "Kebede", "Addis Ababa");
        Customer b = customer(customerBUser, "CUS-000002", "Sara", "Tesfaye", "Adama");

        Account aSav = account("1000000001", a, AccountType.SAVINGS, new BigDecimal("50000.00"));
        Account aCur = account("1000000002", a, AccountType.CURRENT, new BigDecimal("12000.00"));
        Account bSav = account("1000000003", b, AccountType.SAVINGS, new BigDecimal("20000.00"));

        demoTransfer(aSav, bSav, new BigDecimal("1500.00"), customerAUser, "TXN-20260901-DEMO0001", "School fee support");
        demoTransfer(bSav, aCur, new BigDecimal("750.00"), customerBUser, "TXN-20260905-DEMO0002", "Utility share");

        log.info("Demo accounts ready: admin / employee / customer.a / customer.b (see README). Admin id={}", admin.getId());
    }

    private void ensureCatalog() {
        if (roles.count() == 0) {
            role("CUSTOMER", "Customer");
            role("BANK_EMPLOYEE", "Bank Employee");
            role("ADMINISTRATOR", "Administrator");
        }
        if (permissions.count() == 0) {
            String[][] perms = {
                    {"AUTH_LOGIN", "Login"},
                    {"CUSTOMER_READ_OWN", "Read own data"},
                    {"CUSTOMER_TRANSFER", "Transfer"},
                    {"STAFF_CUSTOMERS", "Manage customers"},
                    {"STAFF_ACCOUNTS", "Manage accounts"},
                    {"STAFF_TRANSACTIONS", "Monitor transactions"},
                    {"STAFF_REPORTS", "Staff reports"},
                    {"ADMIN_USERS", "Manage users"},
                    {"ADMIN_ROLES", "Manage roles"},
                    {"ADMIN_AUDIT", "Audit access"},
                    {"ADMIN_SETTINGS", "System settings"}
            };
            for (String[] p : perms) {
                Permission permission = new Permission();
                permission.setCode(p[0]);
                permission.setName(p[1]);
                permissions.save(permission);
            }
            Role admin = roles.findByCode("ADMINISTRATOR").orElseThrow();
            admin.getPermissions().addAll(permissions.findAll());
            roles.save(admin);
        }
        if (settings.count() == 0) {
            setting("max.failed.logins", "5", "Lock after failed logins");
            setting("lockout.minutes", "15", "Lockout minutes");
            setting("transfer.max.amount", "1000000.00", "Max transfer ETB");
            setting("environment.label", "DEMO / TEST", "Banner");
        }
    }

    private void role(String code, String name) {
        Role r = new Role();
        r.setCode(code);
        r.setName(name);
        roles.save(r);
    }

    private void setting(String key, String value, String description) {
        SystemSetting s = new SystemSetting();
        s.setKey(key);
        s.setValue(value);
        s.setDescription(description);
        settings.save(s);
    }

    private UserAccount user(String username, String email, String name, String password, Role role) {
        UserAccount u = new UserAccount();
        u.setUsername(username);
        u.setEmail(email);
        u.setFullName(name);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setStatus(UserStatus.ACTIVE);
        u.getRoles().add(role);
        return users.save(u);
    }

    private Customer customer(UserAccount user, String number, String first, String last, String city) {
        Customer c = new Customer();
        c.setUser(user);
        c.setCustomerNumber(number);
        c.setFirstName(first);
        c.setLastName(last);
        c.setPhone("+251900000000");
        c.setDateOfBirth(LocalDate.of(1992, 3, 15));
        c.setAddressLine("Fictional street 1");
        c.setCity(city);
        c.setStatus(CustomerStatus.ACTIVE);
        return customers.save(c);
    }

    private Account account(String number, Customer customer, AccountType type, BigDecimal balance) {
        Account a = new Account();
        a.setAccountNumber(number);
        a.setCustomer(customer);
        a.setAccountType(type);
        a.setCurrency("ETB");
        a.setBalance(balance);
        a.setStatus(AccountStatus.ACTIVE);
        return accounts.save(a);
    }

    private void demoTransfer(Account from, Account to, BigDecimal amount, UserAccount actor, String reference, String description) {
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accounts.save(from);
        accounts.save(to);
        BankTransaction t = new BankTransaction();
        t.setReference(reference);
        t.setSourceAccount(from);
        t.setDestinationAccount(to);
        t.setAmount(amount);
        t.setCurrency("ETB");
        t.setTransactionType(TransactionType.TRANSFER);
        t.setStatus(TransactionStatus.COMPLETED);
        t.setDescription(description);
        t.setCreatedBy(actor);
        t.setCompletedAt(Instant.now());
        transactions.save(t);
    }
}
