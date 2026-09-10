# Test cases

| Test ID | Description | Precondition | Input | Expected Result | Actual Result | Status |
|---------|-------------|--------------|-------|-----------------|---------------|--------|
| AUTH-01 | Valid login | Seeded customer.a | DemoCustomer#2026 | 200 + token | Automated | See `mvn test` |
| AUTH-02 | Invalid password | Seeded user | wrong password | 401 generic | Automated | |
| AUTH-03 | Unauthenticated API | None | GET /api/accounts | 401 | Automated | |
| AUTHZ-01 | Customer hits admin | Customer token | GET /api/admin/users | 403 | Automated | |
| AUTHZ-02 | Other customer's account | Customer A token | GET /api/accounts/1000000003 | 403 | Automated | |
| TXN-01 | Transfer 5000 | Balances 50k/20k style | 5000 ETB | 45k/25k | Unit | |
| TXN-02 | Insufficient funds | 50k source | 99999 | 422, unchanged | Unit | |
| TXN-03 | Blocked source | BLOCKED | 10 ETB | 422 | Unit | |
| TXN-04 | Missing destination | — | dest 999 | 404 | Unit | |
| TXN-05 | Idempotent replay | Existing key | same key | Same reference, no second debit | Unit | |

Run: `cd backend && mvn test`
