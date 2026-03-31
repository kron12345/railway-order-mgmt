# Railway Order Management

Order management system for railway operations, built with Vaadin Flow, Spring Boot, and PostgreSQL.

## Current Capabilities

- `/orders`: compact accordion overview with summary metrics, inline comment preview, status chips with counters, and per-order filtering of order positions
- `/orders/{id}`: order detail with full order metadata, enriched position rows, and purchase-calendar toggle per position
- `LEISTUNG` order positions: dialog-based editing with predefined tag selection, operational points, required start/end times, validity calendar, and comment
- `FAHRPLAN` order positions: full-screen two-step timetable builder with route calculation, OpenStreetMap preview, TTT-like arrival/departure modes, and archived timetable persistence
- `/settings`: topology CSV import for CHE/DE plus CSV import for the predefined tag catalog

## Prerequisites

- Java 21
- Maven 3.9+ or `./mvnw`
- PostgreSQL 16
- Keycloak 26
- Optional: Docker and Docker Compose for local PostgreSQL + Keycloak

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

## Order Position Types

### LEISTUNG

- Created and edited in a dialog from the order detail view
- Fields: name, service type, `from`/`to` operational points, required start/end times, validity dates, predefined tags, comment
- Time inputs use `TimePicker`; persisted values land in `order_positions.start` and `order_positions.end`
- Validity is stored as JSON segments in `order_positions.validity`

### FAHRPLAN

- Created and edited in the full-screen builder at `/orders/{orderId}/timetable-builder`
- Step 1 defines `from`/`to`/ordered `via` points plus one anchor time: either departure at origin or arrival at destination
- Route is calculated as the shortest path over imported `sections_of_line.length_meters`, bidirectional, with an assumed speed of `70 km/h`
- Step 2 exposes the complete route as a timetable table with halt/activity handling and arrival/departure modes `NONE`, `EXACT`, `WINDOW`
- Saving creates or updates one archived timetable in `timetable_archives` and links it 1:1 through a `CAPACITY` resource need on the order position

## Seed and Import Data

| Purpose | Path | Usage |
|---|---|---|
| Predefined tag catalog | `data/seeds/predefined-tags.csv` | Import via `Settings -> Tags`; no SQL seed inserts are kept in the migration |
| ERA RINF topology | `data/rinf/*.csv` | Import via `Settings -> Topology`; DE operational points are deduplicated by `uopid` during import |
| CH/DE routing connectors | `V7__timetable_archive_and_border_connectors.sql` | Adds synthetic `0m` border connectors for Basel Bad Bf, Schaffhausen, Konstanz, and Kreuzlingen to keep routing across the imported graph possible |

## Tech Stack

- **Vaadin Flow 24.7.4**: server-side UI framework
- **Spring Boot 3.4**: application framework
- **Spring Security**: OAuth2/OIDC authentication with Keycloak
- **PostgreSQL 16**: database
- **Flyway**: database migrations
- **Hibernate Envers**: audit trail
- **Leaflet + OpenStreetMap**: route map in the timetable builder
- **i18n**: German, English, Italian, French

## Project Structure

```text
src/main/java/com/ordermgmt/railway/
├── domain/
│   ├── business/        # Business entities and links
│   ├── customer/        # Customer model
│   ├── infrastructure/  # Operational points, sections of line, tag catalog, imports
│   ├── order/           # Orders, positions, resource needs, purchases
│   └── timetable/       # Timetable routing, archive, row model
├── infrastructure/      # Security, i18n, push, config
└── ui/                  # Vaadin views, layouts, components
```
