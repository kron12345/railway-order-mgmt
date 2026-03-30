# Projektgedaechtnis — Railway Order Management

> Dieses Dokument wird von Claude bei jeder Aenderung automatisch aktualisiert.

## Letzte Aktualisierung
**2026-03-30** — Quality-Tools + CI/CD + .env Konfiguration

## Projektstatus: Setup + Quality Gates abgeschlossen

## Module / Bounded Contexts

| Modul | Status | Entities | Views | Bemerkung |
|---|---|---|---|---|
| **Order** | Skeleton | `Order`, `OrderStatus` | — | Entity + Repository, Envers Audit aktiv |
| **Customer** | Leer | — | — | Package-Struktur vorhanden |
| **Railcar** | Leer | — | — | Package-Struktur vorhanden |
| **Route** | Leer | — | — | Package-Struktur vorhanden |

## Infrastruktur

| Komponente | Status | Details |
|---|---|---|
| **Security (Keycloak)** | Skeleton | SecurityConfig mit OIDC + Keycloak Role Mapping |
| **i18n** | Grundstruktur | TranslationProvider + 4 Sprachen (de/en/it/fr) |
| **Push / Live Updates** | Skeleton | BroadcastService implementiert, @Push aktiv |
| **Audit Trail** | Konfiguriert | Hibernate Envers, Order Entity @Audited |
| **Datenbank** | Konfiguriert | Flyway V1 Migration (orders + audit tables) |
| **Docker** | Bereit | PostgreSQL 16 + Keycloak 26, docker-compose.yml |
| **Frontend Theme** | Grundstruktur | Tailwind CSS via styles.css |
| **.env Konfiguration** | Bereit | .env (gitignored) + .env.example als Vorlage |

## Quality Gates

| Tool | Status | Beschreibung |
|---|---|---|
| **Spotless** | Aktiv | Google Java Style (AOSP), CI-enforced |
| **ArchUnit** | Aktiv | DDD-Schichtregeln, Naming, Annotations (2 Test-Klassen) |
| **JaCoCo** | Aktiv | Min. 60% Line Coverage |
| **SpotBugs** | Aktiv | Statische Analyse, fail on Medium+ |
| **OWASP Dep Check** | Aktiv | CVE-Scanning bei PRs (CI) |
| **Codex Review Hook** | Aktiv | Auto Code Review nach jedem Commit |
| **Security Audit Script** | Bereit | `scripts/security-audit.sh` via Codex CLI |
| **Gitleaks** | Aktiv | Secret Scanning in CI (jeder Push/PR) |
| **Dependabot** | Aktiv | Automatische Dependency-Updates (weekly) |

## CI/CD

| Pipeline | Trigger | Steps |
|---|---|---|
| **ci.yml (build)** | Push/PR auf main | Compile → Spotless → Tests → ArchUnit → SpotBugs → JaCoCo |
| **ci.yml (secrets)** | Push/PR auf main | Gitleaks Secret Detection |
| **ci.yml (security)** | PR auf main | OWASP Dependency Check |
| **Dependabot** | Woechentlich (Mo) | Maven, GitHub Actions, Docker Updates |

## Repo-Ausstattung

| Datei | Zweck |
|---|---|
| `.editorconfig` | Einheitliche Editor-Einstellungen |
| `.env.example` | Umgebungsvariablen-Vorlage |
| `mvnw` / `.mvn/` | Maven Wrapper (reproduzierbare Builds) |
| `CONTRIBUTING.md` | Contributor Guide |
| `SECURITY.md` | Security Policy |
| `.github/pull_request_template.md` | PR Checklist |
| `.github/dependabot.yml` | Dependency Updates |

## UI

| View | Status | Route |
|---|---|---|
| **MainLayout** | Implementiert | — |
| **LoginView** | Implementiert | `/login` |
| **DashboardView** | Noch nicht | `/` |
| **OrderListView** | Noch nicht | `/orders` |

## Datenbank-Migrationen

| Version | Datei | Inhalt |
|---|---|---|
| V1 | `V1__create_schema.sql` | orders, orders_audit, revinfo |

## Offene TODOs
- [ ] Dashboard View implementieren
- [ ] Order CRUD Views (Liste, Detail, Formular)
- [ ] Customer Entity + Views
- [ ] Railcar Entity + Views
- [ ] Route Entity + Views
- [ ] GitHub Wiki befuellen
- [ ] Weitere Flyway Migrationen (customer, railcar, route)

## Bekannte Issues
- Tailwind CSS Feature Flag in Vaadin 24.6 nicht unterstuetzt (direkt via CSS integriert)

## Architektur-Entscheidungen (ADRs)
1. **ADR-001**: Spring Security OAuth2 statt Keycloak Adapter
2. **ADR-002**: Flyway statt Liquibase
3. **ADR-003**: Hibernate Envers fuer Audit Trail

## Technische Schulden
- Keine aktuell

## Changelog
| Datum | Aenderung |
|---|---|
| 2026-03-30 | Repo-Ausstattung: .editorconfig, mvnw, CONTRIBUTING.md, SECURITY.md, Dependabot, Gitleaks |
| 2026-03-30 | Quality-Tools: Spotless, ArchUnit, JaCoCo, SpotBugs, OWASP Dep Check |
| 2026-03-30 | CI/CD: GitHub Actions Pipeline (ci.yml) mit allen Quality Gates |
| 2026-03-30 | .env Konfiguration: .env.example + .env (gitignored) |
| 2026-03-30 | PR Template, Mermaid-Diagramme in ARCHITECTURE.md |
| 2026-03-30 | Codex Review Hook + Security Audit Script |
| 2026-03-30 | Verhaltensregeln in CLAUDE.md (Projektgedaechtnis, Code Review, Security) |
| 2026-03-30 | Initiale Projektstruktur: Maven, Vaadin 24.6, Spring Boot 3.4, DDD, Security, i18n, Push, Envers, Docker |
