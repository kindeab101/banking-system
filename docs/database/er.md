# Database design

## ER (conceptual)

User 1—1 Customer (customers only)  
Customer 1—* Account  
Account 1—* BankTransaction (as source or destination)  
User *—* Role *—* Permission  
User 1—* AuditLog  
User 1—* RefreshToken  
User 1—* Notification  
SystemSetting is a key/value catalog  

## Balance strategy

**Stored balance** on `accounts.balance` plus an immutable-style **transaction row**. Chosen so students can lock two rows and keep a single `@Transactional` boundary. A pure ledger (balance = SUM) is more “textbook” but harder to lock correctly under concurrency.

## Indexes (why)

| Index | Why |
|-------|-----|
| `users.username`, `users.email` | Login |
| `accounts.account_number` | Transfer lookup |
| `accounts.customer_id` | Customer account list |
| `transactions.reference` | User-facing lookup |
| source/dest + `created_at` | History and statements |
| `audit_logs.created_at` | Admin monitoring |

## Constraints

Unique account numbers and transaction references; `balance >= 0`; `amount > 0`; enum CHECKs; FKs for referential integrity.
