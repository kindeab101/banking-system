-- Catalog data (not user passwords). Demo users are created by DataSeeder with BCrypt.

INSERT INTO roles (code, name, description) VALUES
    ('CUSTOMER', 'Customer', 'Access own accounts and simulated transfers'),
    ('BANK_EMPLOYEE', 'Bank Employee', 'Operational customer and account management'),
    ('ADMINISTRATOR', 'Administrator', 'User, role, audit, and system administration');

INSERT INTO permissions (code, name, description) VALUES
    ('AUTH_LOGIN', 'Login', 'Authenticate'),
    ('CUSTOMER_READ_OWN', 'Read own data', 'View own profile, accounts, transactions'),
    ('CUSTOMER_TRANSFER', 'Transfer', 'Initiate simulated transfers'),
    ('STAFF_CUSTOMERS', 'Manage customers', 'Search and manage customers'),
    ('STAFF_ACCOUNTS', 'Manage accounts', 'Create and change account status'),
    ('STAFF_TRANSACTIONS', 'Monitor transactions', 'View and simulate staff postings'),
    ('STAFF_REPORTS', 'Staff reports', 'Operational reports'),
    ('ADMIN_USERS', 'Manage users', 'Create and update system users'),
    ('ADMIN_ROLES', 'Manage roles', 'Assign roles and permissions'),
    ('ADMIN_AUDIT', 'Audit access', 'Read audit logs'),
    ('ADMIN_SETTINGS', 'System settings', 'Change system configuration');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CUSTOMER' AND p.code IN ('AUTH_LOGIN', 'CUSTOMER_READ_OWN', 'CUSTOMER_TRANSFER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'BANK_EMPLOYEE' AND p.code IN (
    'AUTH_LOGIN', 'STAFF_CUSTOMERS', 'STAFF_ACCOUNTS', 'STAFF_TRANSACTIONS', 'STAFF_REPORTS'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATOR';

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('max.failed.logins', '5', 'Lock account after this many consecutive failed logins'),
    ('lockout.minutes', '15', 'Lockout duration in minutes'),
    ('transfer.max.amount', '1000000.00', 'Maximum simulated transfer amount in ETB'),
    ('access.token.minutes', '15', 'JWT access token lifetime'),
    ('environment.label', 'DEMO / TEST', 'Displayed environment banner');
