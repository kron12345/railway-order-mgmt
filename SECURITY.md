# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| 0.x (current) | Yes |

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly.

**Do NOT open a public GitHub issue for security vulnerabilities.**

Instead, please send an email to: **security@railway-ordermgmt.local**

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

We will acknowledge your report within 48 hours and provide a timeline for resolution.

## Security Measures

This project implements the following security measures:

### Authentication & Authorization
- Keycloak OIDC for authentication (no custom password handling)
- Role-based access control (RBAC) via Spring Security
- CSRF protection via Vaadin's built-in mechanisms

### Code Quality & Scanning
- **SpotBugs** — Static analysis for security bugs
- **OWASP Dependency Check** — CVE scanning on every PR
- **Gitleaks** — Secret scanning in CI
- **Codex Security Audit** — Manual penetration testing via CLI

### Data Protection
- All database credentials externalized via environment variables
- No secrets in source code (enforced by Gitleaks)
- Hibernate Envers audit trail for all data changes
- Input validation at system boundaries

### Infrastructure
- PostgreSQL with parameterized queries (JPA/Hibernate)
- Docker containers for isolated development
- Flyway for controlled schema migrations

## Dependencies

We use Dependabot to keep dependencies up-to-date and monitor for known vulnerabilities.
