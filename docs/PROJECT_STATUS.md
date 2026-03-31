# Projektgedaechtnis — Railway Order Management

> Dieses Dokument wird von Claude bei jeder Aenderung automatisch aktualisiert.

## Letzte Aktualisierung
**2026-03-31** — Vaadin 24.7.4, Keycloak Rollen-Fix, Security Audit, A11y, Gemini UI Review

## Projektstatus: Order CRUD + Bestellkalender + RINF Import + Security Hardened

## Module / Bounded Contexts

| Modul | Status | Entities | Views | Bemerkung |
|---|---|---|---|---|
| **Order** | CRUD aktiv | `Order`, `OrderPosition`, `ResourceNeed`, `PurchasePosition` + 7 Enums | OrderListView (Accordion+Heatmap), OrderDetailView (Option B), OrderPositionDialog | Konzept C, Bestellkalender mit TTR-Phasen |
| **Customer** | Entity | `Customer`, `CustomerStatus` | — | Repository vorhanden |
| **Business** | Entity | `Business`, `BusinessStatus` | — | Repository vorhanden, eigene Seite geplant |
| **Infrastructure** | Import aktiv | `OperationalPoint`, `SectionOfLine`, `ImportLog` | SettingsView (@RolesAllowed ADMIN) | ERA RINF: 19.321 OPs + 13.849 SoLs (CH+DE) |
| **Railcar** | Leer | — | — | Package-Struktur vorhanden |
| **Route** | Leer | — | — | Package-Struktur vorhanden |

## Infrastruktur

| Komponente | Status | Details |
|---|---|---|
| **Vaadin** | 24.7.4 | Upgrade von 24.6.7, CVE-2026-2742 behoben |
| **Security (Keycloak)** | Produktionsreif | OIDC + Realm Role Mapping (id.token.claim=true), @RolesAllowed funktioniert |
| **Rollen** | Verifiziert | sebastian=ADMIN (Settings ✓), dispatcher=DENIED, viewer=DENIED |
| **i18n** | 80+ Keys | TranslationProvider + 4 Sprachen (de/en/it/fr), inkl. Bestellkalender + Settings |
| **Push / Live Updates** | Skeleton | BroadcastService implementiert, @Push aktiv |
| **Audit Trail** | Konfiguriert | Hibernate Envers, Order + Position @Audited |
| **Datenbank** | V1-V3 Migrationen | orders, customers, positions, resources, purchases, businesses, infrastructure, import_log |
| **Docker** | Bereit | PostgreSQL 16 + Keycloak 26, docker-compose.yml |
| **Frontend Theme** | Bloomberg Amber | Dark Theme, #FFB800 Accent, JetBrains Mono, Terminal-Density |
| **Accessibility** | WCAG 2.1 AA | Kontrast, tabindex, ARIA, Focus, reduced-motion, Status-Symbole |
| **.env Konfiguration** | Bereit | .env (gitignored) + .env.example als Vorlage |

## Security Audit

| Finding | Severity | Status |
|---|---|---|
| Upload ohne Size-Limit | HIGH | ✅ 50MB Limit |
| Settings fuer alle User | HIGH | ✅ @RolesAllowed("ADMIN") |
| Vaadin CVE-2026-2742 | MEDIUM | ✅ 24.7.4 |
| Import partiell-destruktiv | MEDIUM | ✅ Atomic delete+insert |
| Fehlende Bean-Validation | MEDIUM | ✅ @NotBlank/@Size/@NotNull |
| JSON Range-Limit | MEDIUM | ✅ ChronoUnit.DAYS.between |
| Error-Message Sanitizing | LOW | ✅ sanitizeError() |

## Quality Gates

| Tool | Status | Beschreibung |
|---|---|---|
| **Spotless** | Aktiv | Google Java Style (AOSP), CI-enforced |
| **ArchUnit** | Aktiv | DDD-Schichtregeln, Naming, Annotations |
| **JaCoCo** | Aktiv | Min. 60% Line Coverage |
| **SpotBugs** | Aktiv | Statische Analyse, fail on Medium+ |
| **OWASP Dep Check** | Aktiv | CVE-Scanning bei PRs (CI) |
| **Codex Review** | Aktiv | Security + Code Quality Reviews |
| **Gemini Review** | Durchgefuehrt | UI/UX + A11y Audit, 10/10 Bewertung |
| **Gitleaks** | Aktiv | Secret Scanning in CI |
| **Dependabot** | Aktiv | Automatische Dependency-Updates |

## UI

| View | Route | Status | Beschreibung |
|---|---|---|---|
| **MainLayout** | — | Implementiert | Drawer-Nav + Breadcrumbs + Sprachwechsel |
| **LoginView** | `/login` | Implementiert | Keycloak SSO Redirect |
| **DashboardView** | `/` | Implementiert | KPI-Cards (Platzhalter) |
| **OrderListView** | `/orders` | Implementiert | Accordion + Status-Heatmap + Kacheln + FPJ-Filter |
| **OrderDetailView** | `/orders/{id}` | Implementiert | Option B: Kompakter Header + Positionen + Bestellkalender |
| **SettingsView** | `/settings` | Implementiert | RINF Import + Datenbestand + Import-Verlauf (ADMIN only) |

## UI-Komponenten

| Komponente | Beschreibung |
|---|---|
| `StatusBadge` | Farbcodiertes Status-Pill |
| `StatusHeatmap` | Farbige Zellen mit Symbolen pro Position (A11y) |
| `PositionTile` | Positions-Kachel mit Route, Typ, Ressourcen-Tooltips |
| `PurchaseCalendarPanel` | Bestellkalender mit TTR-Phasen + Summary |
| `PurchaseCalendarGrid` | Kompakte Monatszeilen, Crosshair-Hover, ARIA |
| `PurchaseDetailTable` | Scrollbare Detail-Tabelle der Bestellpositionen |
| `OrderFormPanel` | Auftragsformular (Dialog-basiert) |
| `OrderPositionRow` | Positionszeile mit Kalender-Toggle |
| `OrderPositionDialog` | Positions-CRUD-Dialog |

## Datenbank-Migrationen

| Version | Datei | Inhalt |
|---|---|---|
| V1 | `V1__create_schema.sql` | orders, orders_audit, revinfo |
| V2 | `V2__expand_domain_model.sql` | customers, order_positions, resource_needs, purchase_positions, businesses + audit |
| V3 | `V3__infrastructure_tables.sql` | operational_points, sections_of_line, import_log |

## ERA RINF Infrastrukturdaten

| Land | Operational Points | Sections of Line |
|---|---|---|
| Schweiz (CHE) | 3.261 | 1.588 |
| Deutschland (DEU) | 16.060 | 12.261 |
| **Total** | **19.321** | **13.849** |

Quelle: ERA SPARQL Endpoint `https://era.linkeddata.es/sparql`

## Offene TODOs
- [ ] Customer Views
- [ ] Business/Geschaeft Views (eigene Seite)
- [ ] Fahrplanbuilder-Komponente (OSM + RINF OPs)
- [ ] Lazy Loading / Pagination bei grossen Datenmengen
- [ ] N+1 Problem in OrderService (EntityGraph / JOIN FETCH)
- [ ] Erweiterte E2E-Tests (Kalender, Import, Error-States)

## Bekannte Issues
- ~~Schwarze Seite nach Keycloak-Login~~ (behoben)
- ~~Vaadin CVE-2026-2742~~ (behoben: 24.7.4)
- ~~Keycloak Rollen nicht im ID-Token~~ (behoben: id.token.claim=true)

## Architektur-Entscheidungen (ADRs)
1. **ADR-001**: Spring Security OAuth2 statt Keycloak Adapter
2. **ADR-002**: Flyway statt Liquibase
3. **ADR-003**: Hibernate Envers fuer Audit Trail
4. **ADR-004**: Konzept C (Accordion + Heatmap + Kacheln) fuer Auftragsliste
5. **ADR-005**: Option B (Kompakter Header + Dialog) fuer Auftragsdetail
6. **ADR-006**: Bestellkalender mit TTR-Phasen (1 Zeile pro Monat, Wochentag-Spalten)
7. **ADR-007**: ERA RINF als Infrastruktur-Stammdaten (SPARQL Import)
8. **ADR-008**: Bloomberg Amber (#FFB800) Terminal Theme

## Changelog
| Datum | Aenderung |
|---|---|
| 2026-03-31 | Vaadin 24.6.10 → 24.7.4 Upgrade (CVE-2026-2742 behoben) |
| 2026-03-31 | Keycloak Rollen-Fix: id.token.claim=true auf realm/client roles Mapper |
| 2026-03-31 | @RolesAllowed("ADMIN") auf SettingsView, verifiziert mit 3 Usern |
| 2026-03-31 | WCAG 2.1 AA: Kontrast, tabindex, ARIA, Focus-Visible, reduced-motion, Status-Symbole |
| 2026-03-31 | Gemini UI Review: 8 Verbesserungen (Amber Theme, Density, Tooltips, FPJ-Filter, Breadcrumbs, Grid-Header, Crosshair, A11y) |
| 2026-03-31 | Codex Security Audit: 7 Findings gefixt (Upload-Limit, Bean-Validation, Import-Rollback, JSON-Hardening, Error-Sanitizing) |
| 2026-03-31 | Codex Code Cleanup: JavaDoc, i18n, Service-Refactor, Package-Restrukturierung |
| 2026-03-30 | ERA RINF Import: 19.321 OPs + 13.849 SoLs (CH+DE), SettingsView mit CSV-Upload |
| 2026-03-30 | Bestellkalender: Kompakte Monatszeilen, TTR-Phasen, Crosshair-Hover, Toggle pro Position |
| 2026-03-30 | Option B: Kompakter Order-Header + Edit-Dialog, Positionen nutzen vollen Bildschirm |
| 2026-03-30 | Erweitertes Datenmodell (6 Entities, 8 Enums, V2+V3 Migration) |
| 2026-03-30 | Konzept C: Accordion OrderListView mit Status-Heatmap und responsive Kacheln |
| 2026-03-30 | Fix: schwarze Seite nach Keycloak-Login, Playwright E2E Tests |
| 2026-03-30 | Initiale Projektstruktur, Quality Gates, CI/CD, Wiki, Repo-Ausstattung |
