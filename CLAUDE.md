# Railway Order Management - AI Context

## Project Overview
Order management system for railway operations. Vaadin Flow + Spring Boot + PostgreSQL.
GitHub: https://github.com/kron12345/railway-order-mgmt

## Tech Stack
- **Java 21**, **Maven**
- **Vaadin Flow 24.6.x** with Tailwind CSS
- **Spring Boot 3.4.x** with Spring Security OAuth2 (Keycloak OIDC)
- **PostgreSQL 16** with Hibernate/JPA + Flyway migrations
- **Hibernate Envers** for audit trail
- **Lombok** + **MapStruct** for boilerplate reduction

## Architecture
- DDD package structure: `domain/{boundedContext}/{model,repository,service,event}`
- UI layer: `ui/{layout,view,component,converter}`
- Infrastructure: `infrastructure/{security,i18n,push,config}`
- Base package: `com.ordermgmt.railway`

## Key Patterns
- **Authentication**: Spring Security OAuth2 Client with Keycloak (NOT the deprecated Keycloak adapter). Roles mapped from OIDC token claims.
- **i18n**: Vaadin `I18NProvider` with `messages_XX.properties` bundles (de, en, it, fr). Use `getTranslation("key")` in views.
- **Live Updates**: Vaadin `@Push` with `BroadcastService`. Register in `onAttach()`, unregister in `onDetach()`. Always use `ui.access(() -> { })`.
- **Audit**: `@Audited` on entities, `RevisionRepository` for history queries.
- **Styling**: Tailwind CSS for layout/spacing, Vaadin Lumo theme tokens for component theming. Do NOT try to style Vaadin component shadow DOM with Tailwind.

## Commands
- `mvn spring-boot:run` — Start dev server (port 8080)
- `mvn clean compile` — Compile with frontend build
- `mvn -Pproduction package` — Production build
- `docker compose -f docker/docker-compose.yml up -d` — Start PostgreSQL + Keycloak

## Conventions
- Entity classes use Lombok `@Getter @Setter @NoArgsConstructor`
- All entities have UUID primary keys
- Flyway migrations: `V{n}__{description}.sql` in `src/main/resources/db/migration/`
- Translation keys: `{module}.{context}.{name}` (e.g., `order.status.DRAFT`)
- Views are in `ui/view/{module}/` and use `@Route` annotation

---

## Verhaltensregeln für Claude

### 1. Projektgedächtnis aktuell halten
Nach JEDER inhaltlichen Änderung am Projekt (neue Features, geänderte Architektur, neue Entities, geänderte Konfiguration, etc.) MUSS `docs/PROJECT_STATUS.md` aktualisiert werden:
- Was wurde geändert/hinzugefügt
- Aktueller Stand aller Module
- Offene TODOs und bekannte Issues
- Letzte Änderung mit Datum

### 2. Code Review via Codex nach jedem Commit
Nach JEDEM `git commit` wird automatisch ein Code Review via Codex CLI ausgeführt (Hook konfiguriert).
Falls der Hook nicht greift, führe manuell aus:
```bash
codex review --uncommitted
```
oder nach einem Commit:
```bash
codex review --commit HEAD
```
**Reagiere auf Findings:** Wenn Codex Probleme meldet, behebe sie SOFORT bevor du weitermachst.

### 3. Security / Pen Tests via Codex
Führe nach jedem größeren Feature oder sicherheitsrelevanter Änderung einen Security-Scan durch:
```bash
codex exec "Führe einen Security-Audit des Projekts durch. Prüfe auf: OWASP Top 10, SQL Injection, XSS, CSRF, unsichere Konfiguration, hartcodierte Secrets, fehlende Input-Validierung. Fokus auf die zuletzt geänderten Dateien. Gib einen strukturierten Bericht." --full-auto
```
Sicherheitsrelevante Änderungen sind: SecurityConfig, Controller/Views mit User-Input, Datenbankabfragen, OAuth2-Konfiguration, Docker-Konfiguration.

### 4. Qualitätsregeln
- Kein Code ohne Compile-Check (`mvn clean compile`)
- Alle neuen Entities bekommen `@Audited` (Envers)
- Alle User-sichtbaren Texte über i18n (`getTranslation()`)
- Keine hardcodierten Secrets — immer Umgebungsvariablen
- Input-Validierung an allen Systemgrenzen (User-Input, API)
- **Spotless** (Google Java Style AOSP) wird in CI erzwungen
- **ArchUnit** Tests sichern DDD-Schichtregeln und Naming ab
- **JaCoCo** Mindest-Coverage 60%
- **SpotBugs** statische Analyse, fail on Medium+

### 5. Dateigroesse und Struktur
- **KEINE Datei ueber 300-500 Zeilen!** Lieber mehrere kleine Dateien.
- Klassen mit mehreren Concerns aufteilen
- Views mit vielen Komponenten: Teile in eigene Component-Klassen
- Tests thematisch gruppieren, nicht alles in eine Datei

### 6. Konfiguration und Umgebungsvariablen
- **KEINE Variablen/Werte im Code hardcoden!**
- Infrastruktur-Werte (DB, Keycloak, Ports) → `.env` Datei (gitignored)
- `.env.example` als Vorlage im Repo
- `application.yaml` referenziert via `${VAR:default}`
- Geschaeftslogik-Konfiguration → Konfigurationsbereich (Admin-UI / DB)
- Soviel wie moeglich spaeter im GUI konfigurierbar

### 7. Dokumentation
- `docs/PROJECT_STATUS.md` — Lebendes Projektgedächtnis (IMMER aktuell halten)
- `docs/ARCHITECTURE.md` — Bei Architekturänderungen aktualisieren
- `docs/GLOSSARY.md` — Bei neuen Domänenbegriffen ergänzen
- `docs/decisions/` — Bei jeder Architekturentscheidung ein neues ADR
- `CLAUDE.md` — Bei Stack/Konventions-Änderungen aktualisieren
