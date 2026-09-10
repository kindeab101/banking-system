# Security architecture and threat model

Authentication: JWT access token (15 min) + hashed refresh token. Authorization: Spring method security + ownership checks. CSRF: disabled because the API is Bearer-token (not cookie session). Residual XSS risk if `sessionStorage` tokens are stolen. CORS: explicit origins.

## Threats

| Threat | Risk | Mitigation | Residual |
|--------|------|------------|----------|
| Stolen password | High | BCrypt, lockout, generic errors | No MFA |
| Brute force | High | Failed-attempt lock | Distributed attacks |
| Privilege escalation | High | `@PreAuthorize` + tests | Missed annotation |
| IDOR | High | Owner checks | Missed endpoint |
| Double spend / concurrency | High | Pessimistic locks, ordered ids | Forgotten lock |
| Replay transfer | Medium | Idempotency-Key | Client omits key |
| SQLi | Medium | JPA only | Native query later |
| XSS | Medium | React escaping | Dangerous HTML |
| Secret leak | High | Env vars | Local `.env` mishandling |
| Audit tampering | Medium | No update API | DBA access |
