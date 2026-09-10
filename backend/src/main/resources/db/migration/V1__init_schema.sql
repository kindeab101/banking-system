-- Secure Web-Based Banking Management System
-- Flyway V1: relational schema with constraints and indexes
-- DEMO / TEST prototype — simulated financial data only

CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255)
);

CREATE TABLE permissions (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    description     VARCHAR(255)
);

CREATE TABLE role_permissions (
    role_id         BIGINT NOT NULL REFERENCES roles (id),
    permission_id   BIGINT NOT NULL REFERENCES permissions (id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    username              VARCHAR(80)  NOT NULL UNIQUE,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(160) NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    failed_login_count    INTEGER      NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id),
    role_id BIGINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE customers (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL UNIQUE REFERENCES users (id),
    customer_number  VARCHAR(32) NOT NULL UNIQUE,
    first_name       VARCHAR(80) NOT NULL,
    last_name        VARCHAR(80) NOT NULL,
    phone            VARCHAR(30),
    date_of_birth    DATE,
    address_line     VARCHAR(255),
    city             VARCHAR(80),
    status           VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    account_number  VARCHAR(20)    NOT NULL UNIQUE,
    customer_id     BIGINT         NOT NULL REFERENCES customers (id),
    account_type    VARCHAR(20)    NOT NULL,
    currency        CHAR(3)        NOT NULL DEFAULT 'ETB',
    balance         NUMERIC(19, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    opened_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('SAVINGS', 'CURRENT')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0),
    CONSTRAINT chk_accounts_currency CHECK (currency = 'ETB')
);

CREATE TABLE transactions (
    id                      BIGSERIAL PRIMARY KEY,
    reference               VARCHAR(40)    NOT NULL UNIQUE,
    source_account_id       BIGINT         REFERENCES accounts (id),
    destination_account_id  BIGINT         REFERENCES accounts (id),
    amount                  NUMERIC(19, 2) NOT NULL,
    currency                CHAR(3)        NOT NULL DEFAULT 'ETB',
    transaction_type        VARCHAR(32)    NOT NULL,
    status                  VARCHAR(20)    NOT NULL,
    description             VARCHAR(255),
    failure_reason          VARCHAR(255),
    idempotency_key         VARCHAR(80),
    created_by_user_id      BIGINT         REFERENCES users (id),
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMPTZ,
    CONSTRAINT chk_txn_amount CHECK (amount > 0),
    CONSTRAINT chk_txn_type CHECK (transaction_type IN ('TRANSFER', 'DEPOSIT_SIMULATION', 'WITHDRAWAL_SIMULATION')),
    CONSTRAINT chk_txn_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED', 'CANCELLED')),
    CONSTRAINT chk_txn_accounts CHECK (
        source_account_id IS NOT NULL OR destination_account_id IS NOT NULL
    )
);

CREATE UNIQUE INDEX uq_transactions_idempotency
    ON transactions (created_by_user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE audit_logs (
    id                BIGSERIAL PRIMARY KEY,
    actor_user_id     BIGINT REFERENCES users (id),
    action            VARCHAR(80)  NOT NULL,
    entity_type       VARCHAR(80),
    entity_reference  VARCHAR(80),
    result            VARCHAR(20)  NOT NULL,
    ip_address        VARCHAR(45),
    user_agent        VARCHAR(255),
    metadata          TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_audit_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id),
    title       VARCHAR(120) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE system_settings (
    setting_key    VARCHAR(80)  PRIMARY KEY,
    setting_value  VARCHAR(255) NOT NULL,
    description    VARCHAR(255)
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
CREATE INDEX idx_transactions_source ON transactions (source_account_id, created_at);
CREATE INDEX idx_transactions_dest ON transactions (destination_account_id, created_at);
CREATE INDEX idx_transactions_created ON transactions (created_at);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_audit_actor ON audit_logs (actor_user_id);
CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at);
CREATE INDEX idx_customers_name ON customers (last_name, first_name);
