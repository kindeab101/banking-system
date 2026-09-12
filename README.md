# Secure Web-Based Banking Management System

Academic/internship **prototype** for a secure web-based banking platform. All money movement is **simulated**. There is no connection to real customer accounts, payment networks, or production bank systems.

**Environment label:** DEMO / TEST

---

## Project Overview

The system provides three portals in one React application, backed by a Spring Boot REST API and PostgreSQL:

- **Customer portal** — own accounts, balances, history, simulated transfers, statements
- **Bank management portal** — customer/account operations, transaction monitoring, reports
- **Administrator portal** — users, roles, audit logs, settings



## Features

- BCrypt password hashing, JWT access tokens, hashed refresh tokens, lockout after failed logins
- Server-side role-based access control (customers cannot call admin APIs)
- Atomic internal transfers with `NUMERIC`/`BigDecimal`, pessimistic row locks, and idempotency keys
- Append-only audit trail (separate from the financial ledger)
- Flyway migrations, OpenAPI/Swagger, Docker Compose



## Architecture

Browser → React (TypeScript) → REST/JWT → Spring Security → Services → JPA → PostgreSQL

Business rules live in the service layer. API clients receive DTOs, never password hashes.

## Technology Stack


| Area     | Choice                                                     |
| -------- | ---------------------------------------------------------- |
| Frontend | React 18, TypeScript, Vite, React Router, Axios            |
| Backend  | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Database | PostgreSQL 16, Flyway                                      |
| Docs     | springdoc-openapi                                          |
| Tests    | JUnit 5, Mockito, MockMvc, Vitest                          |




## Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 16 (or Docker)
- Git (optional; install Git for Windows if you want version control)



## Installation

```bash
cd banking-system
copy .env.example .env
```

Edit `.env` so `JWT_SECRET` is at least 32 characters. Do not commit `.env`.

## Configuration

See `.env.example`. Backend reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS`.

## Database Setup

Create database `banking` or start Postgres with Compose (below). Flyway applies `V1` schema and `V2` catalog data on startup. Demo users are seeded by `DataSeeder` (BCrypt hashes).

## Running Backend

```bash
cd backend
mvn spring-boot:run
```

API: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Running Frontend

```bash
cd frontend
npm install
npm run dev
```

Portal: `http://localhost:5173` (Vite proxies `/api` to port 8080)

## Running Tests

```bash
cd backend
mvn test

cd frontend
npm test
```



## Docker

```bash
docker compose up --build
```

- Frontend: `http://localhost:8088`
- Backend: `http://localhost:8080`
- Postgres: `localhost:5432`



## API Documentation

Authenticated endpoints expect `Authorization: Bearer <accessToken>`.  
Transfer idempotency: header `Idempotency-Key`. Replay returns the original completed transfer without moving money again.

Business-rule failures use **422 Unprocessable Entity**. Duplicate/idempotency uniqueness uses **409** where a unique constraint is the cause. Validation uses **400**. Missing resources **404**. Unauthenticated **401**. Forbidden **403**.

## Demo Accounts (development only)


| Username     | Password            | Role                    |
| ------------ | ------------------- | ----------------------- |
| `admin`      | `DemoAdmin#2026`    | ADMINISTRATOR           |
| `employee`   | `DemoStaff#2026`    | BANK_EMPLOYEE           |
| `customer.a` | `DemoCustomer#2026` | CUSTOMER (Abebe Kebede) |
| `customer.b` | `DemoCustomer#2026` | CUSTOMER (Sara Tesfaye) |
| `Kindeab` | `Kindeab123` | CUSTOMER (Kinde Abdurahman) |

Fictional accounts include `1000000001` (Abebe savings) and `1000000003` (Sara savings). Try transferring **5000 ETB** from `1000000001` to `1000000003`.

## Security Notes

- Passwords are hashed with BCrypt. JWT signing uses `JWT_SECRET` from the environment.
- Access tokens last 15 minutes; refresh tokens are stored hashed and rotated.
- Stateless JWT: CSRF is not the primary browser threat; XSS remains a residual risk if tokens are stolen from sessionStorage.
- CORS is restricted to configured origins (not `*`).
- Customers are blocked from other customers’ accounts by **service-layer ownership checks**, not by hiding buttons.
- Audit logs have no update/delete API.



## Project Limitations

- Simulated ledger only; not production core banking
- No MFA, no real KYC, no payment gateway, no ATM/cards
- Single-region modular monolith; not high-availability infrastructure
- Demo data is fictional (Ethiopia-style names and ETB amounts)



## Documentation

See `docs/` for UML, ER notes, threat model, test cases, user guides, and report chapter outlines.