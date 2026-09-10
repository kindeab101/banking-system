# UML and analysis (Phase 2)

Prototype: Secure Web-Based Banking Management System (DEMO / TEST).

## Use case diagram

```mermaid
flowchart LR
  C[Customer] --> UC1[Login / logout]
  C --> UC2[View dashboard and accounts]
  C --> UC3[View transactions]
  C --> UC4[Transfer funds]
  C --> UC5[View statement]
  E[Bank Employee] --> UC1
  E --> UC6[Search customers]
  E --> UC7[Manage accounts]
  E --> UC8[Monitor transactions]
  E --> UC9[Generate reports]
  A[Administrator] --> UC1
  A --> UC10[Manage users and roles]
  A --> UC11[View audit logs]
  A --> UC12[System settings]
```

## Activity: login

```mermaid
flowchart TD
  A[Submit credentials] --> B{User found and ACTIVE?}
  B -->|no| C[Generic failure + audit]
  B -->|yes| D{Password matches?}
  D -->|no| E[Increment failures / lock]
  D -->|yes| F[Issue JWT + hashed refresh]
  F --> G[Audit LOGIN_SUCCESS]
```

## Activity: fund transfer

```mermaid
flowchart TD
  A[Customer submits after confirm] --> B{Idempotency key seen?}
  B -->|yes| C[Return original result]
  B -->|no| D[Lock both accounts by id order]
  D --> E{Valid amount, status, funds, owner?}
  E -->|no| F[Fail, no balance change]
  E -->|yes| G[Debit + credit + ledger COMPLETED]
  G --> H[Audit + notify]
```

## Sequence: transfer

```mermaid
sequenceDiagram
  participant UI as React
  participant API as TransferController
  participant S as TransferService
  participant DB as PostgreSQL
  UI->>API: POST /api/transactions/transfer
  API->>S: transfer(actor, dto, idempotencyKey)
  S->>DB: SELECT FOR UPDATE accounts
  S->>DB: UPDATE balances + INSERT transaction
  S-->>API: TransferResponse
  API-->>UI: reference + COMPLETED
```

## Class diagram (principal domain)

```mermaid
classDiagram
  UserAccount "1" -- "*" Role
  Role "*" -- "*" Permission
  UserAccount "1" -- "0..1" Customer
  Customer "1" -- "*" Account
  Account "1" -- "*" BankTransaction : source
  Account "1" -- "*" BankTransaction : destination
  UserAccount "1" -- "*" AuditLog
  UserAccount "1" -- "*" RefreshToken
```
