# Railway Order Management

Order management system for railway operations, built with Vaadin Flow, Spring Boot, and PostgreSQL.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for PostgreSQL + Keycloak)

## Quick Start

```bash
# Start infrastructure (PostgreSQL + Keycloak)
docker compose -f docker/docker-compose.yml up -d

# Run the application
mvn spring-boot:run
```

The app starts at http://localhost:8080.
Keycloak admin console at http://localhost:8180 (admin/admin).

### Test Users

| Username | Password | Role |
|---|---|---|
| admin | admin | admin |
| dispatcher | dispatcher | dispatcher |

## Build

```bash
# Development
mvn clean compile

# Production
mvn -Pproduction clean package
```

## Tech Stack

- **Vaadin Flow 24.6** — Server-side UI framework with Tailwind CSS
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
