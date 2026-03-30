# Architecture Overview

## System Context

```
[Browser] <--HTTPS--> [Vaadin Flow App] <--JDBC--> [PostgreSQL]
                            |
                            +--OIDC--> [Keycloak]
```

## Layers

### UI Layer (`ui/`)
- **Vaadin Flow** server-side UI framework
- `layout/` — AppLayout-based MainLayout with navigation + language switcher
- `view/` — Route-annotated views per module (dashboard, order, customer, railcar, route)
- `component/` — Reusable custom components
- Styling: Tailwind CSS utilities + Vaadin Lumo theme

### Domain Layer (`domain/`)
- Organized by **Bounded Context** (DDD)
- Each context has: `model/`, `repository/`, `service/`, `event/`
- Bounded Contexts: **Order**, **Customer**, **Railcar**, **Route**

### Infrastructure Layer (`infrastructure/`)
- `security/` — Spring Security + Keycloak OIDC configuration
- `i18n/` — TranslationProvider (de, en, it, fr)
- `push/` — BroadcastService for live updates via WebSocket
- `config/` — Application configuration beans

## Authentication Flow
1. User navigates to protected route
2. Spring Security redirects to Keycloak login
3. Keycloak authenticates, returns OIDC token
4. `SecurityConfig` maps Keycloak realm/client roles to Spring GrantedAuthorities
5. Vaadin views use `@RolesAllowed` for authorization

## Live Updates (Push)
1. `@Push` annotation enables WebSocket connection
2. `BroadcastService` manages event listeners per event type
3. Views register in `onAttach()`, unregister in `onDetach()`
4. UI updates via `ui.access(() -> { ... })` for thread safety

## Audit Trail
- Hibernate Envers: `@Audited` entities get `_audit` shadow tables
- Every change creates a revision in `revinfo` table
- Query history via `RevisionRepository`

## Database
- PostgreSQL 16 with Flyway migrations
- Schema managed in `src/main/resources/db/migration/`
- HikariCP connection pool (Spring Boot default)
