# Railway Order Management

Order management system for railway operations, built with Vaadin Flow, Spring Boot, and PostgreSQL.

## Current Capabilities

- `/orders`: compact accordion overview with summary metrics, inline comment preview, status chips with counters, and per-order position filtering
- `/orders/new` and edit dialog: predefined tag selection from the catalog (`ORDER` / `GENERAL`), while existing free-form legacy tags remain intact
- `/settings`: topology CSV import for CHE/DE plus CSV import for the predefined tag catalog

## Prerequisites

- Java 21
- Maven 3.9+ or `./mvnw`
- PostgreSQL 16
- Keycloak 26
- Optional: Docker & Docker Compose for local PostgreSQL + Keycloak

## Quick Start

```bash
# Create local environment file
cp .env.example .env

# Option A: use existing local PostgreSQL + Keycloak from .env
# Option B: start helper containers
docker compose -f docker/docker-compose.yml up -d

# Run the application
./mvnw spring-boot:run
```

The app starts at http://localhost:8085.
Keycloak admin console at http://localhost:8180 (admin/admin).

### Test Users

| Username | Password | Role |
|---|---|---|
| admin | admin | admin |
| dispatcher | dispatcher | dispatcher |

## Build

```bash
# Development
./mvnw clean compile

# Production
./mvnw -Pproduction clean package
```

## Seed and Import Data

| Purpose | Path | Usage |
|---|---|---|
| Predefined tag catalog | `data/seeds/predefined-tags.csv` | Import via `Settings -> Tags`; no SQL seed inserts are kept in the migration |
| ERA RINF topology | `data/rinf/*.csv` | Import via `Settings -> Topology`; DE operational points are deduplicated by `uopid` during import |

## Tech Stack

- **Vaadin Flow 24.7.4** — Server-side UI framework
- **Spring Boot 3.4** — Application framework
- **Spring Security** — OAuth2/OIDC authentication with Keycloak
- **PostgreSQL 16** — Database
- **Flyway** — Database migrations
- **Hibernate Envers** — Audit trail
- **i18n** — German, English, Italian, French

## Project Structure

```
src/main/java/com/ordermgmt/railway/
├── domain/          # DDD bounded contexts (order, customer, railcar, route)
├── ui/              # Vaadin views, layouts, components
├── infrastructure/  # Security, i18n, push, config
├── dto/             # Data transfer objects
└── mapper/          # MapStruct mappers
```
