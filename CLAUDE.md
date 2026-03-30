# Railway Order Management - AI Context

## Project Overview
Order management system for railway operations. Vaadin Flow + Spring Boot + PostgreSQL.

## Tech Stack
- **Java 21**, **Maven**
- **Vaadin Flow 24.6.x** with Tailwind CSS (experimental feature flag)
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
