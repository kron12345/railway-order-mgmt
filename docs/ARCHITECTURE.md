# Architecture Overview

## System Context

```mermaid
graph LR
    Browser["Browser"] -->|HTTPS| App["Vaadin Flow App<br/>:8080"]
    App -->|JDBC| DB["PostgreSQL<br/>:5432"]
    App -->|OIDC| KC["Keycloak<br/>:8180"]
    App -->|WebSocket| Browser
```

## Layer Architecture

```mermaid
graph TB
    subgraph UI["UI Layer"]
        Layout["MainLayout"]
        Views["Views<br/>(Dashboard, Order, Customer, Railcar, Route)"]
        Components["Reusable Components<br/>(LanguageSwitcher, StatusBadge)"]
    end

    subgraph Domain["Domain Layer (DDD)"]
        Order["Order Context<br/>model / repository / service / event"]
        Customer["Customer Context"]
        Railcar["Railcar Context"]
        Route["Route Context"]
    end

    subgraph Infra["Infrastructure Layer"]
        Security["SecurityConfig<br/>(OAuth2/OIDC)"]
        I18n["TranslationProvider<br/>(de/en/it/fr)"]
        Push["BroadcastService<br/>(WebSocket Push)"]
        Config["Config Beans"]
    end

    subgraph Data["Data Layer"]
        DB2["PostgreSQL"]
        Flyway["Flyway Migrations"]
        Envers["Hibernate Envers<br/>(Audit Trail)"]
    end

    UI --> Domain
    UI --> Infra
    Infra --> Domain
    Domain --> Data

    style Domain fill:#e1f5fe
    style UI fill:#f3e5f5
    style Infra fill:#fff3e0
    style Data fill:#e8f5e9
```

## Authentication Flow

```mermaid
sequenceDiagram
    participant B as Browser
    participant V as Vaadin App
    participant S as Spring Security
    participant K as Keycloak

    B->>V: Navigate to /orders
    V->>S: Check authentication
    S-->>B: 302 Redirect to Keycloak
    B->>K: Login (username/password)
    K-->>B: Authorization Code
    B->>V: Callback with code
    V->>K: Exchange code for tokens
    K-->>V: ID Token + Access Token
    V->>S: Map Keycloak roles to GrantedAuthorities
    S-->>V: Authenticated session
    V-->>B: Render /orders
```

## Live Updates (Push)

```mermaid
sequenceDiagram
    participant U1 as User A (Browser)
    participant V1 as Vaadin UI (A)
    participant BS as BroadcastService
    participant V2 as Vaadin UI (B)
    participant U2 as User B (Browser)

    U1->>V1: Update order status
    V1->>BS: broadcast(OrderStatusChanged)
    BS->>V2: notify listener
    V2->>V2: ui.access(() -> updateGrid())
    V2-->>U2: Grid refreshed (WebSocket Push)
```

## Bounded Contexts (DDD)

```mermaid
graph LR
    subgraph Order["Order Context"]
        O_M["Order<br/>OrderItem<br/>OrderStatus"]
        O_R["OrderRepository"]
        O_S["OrderService"]
        O_E["OrderCreatedEvent"]
    end

    subgraph Customer["Customer Context"]
        C_M["Customer"]
        C_R["CustomerRepository"]
        C_S["CustomerService"]
    end

    subgraph Railcar["Railcar Context"]
        R_M["Railcar<br/>RailcarType"]
        R_R["RailcarRepository"]
        R_S["RailcarService"]
    end

    subgraph Route["Route Context"]
        RT_M["Route<br/>Station"]
        RT_R["RouteRepository"]
        RT_S["RouteService"]
    end

    Order -.->|references| Customer
    Order -.->|references| Railcar
    Order -.->|references| Route

    style Order fill:#e3f2fd
    style Customer fill:#fce4ec
    style Railcar fill:#f1f8e9
    style Route fill:#fff8e1
```

## Audit Trail (Envers)

```mermaid
graph LR
    Entity["@Audited Entity"] -->|INSERT/UPDATE/DELETE| Envers["Hibernate Envers"]
    Envers --> AuditTable["orders_audit"]
    Envers --> RevInfo["revinfo<br/>(revision + timestamp)"]
    Query["RevisionRepository"] --> AuditTable
    Query --> RevInfo
```

## CI/CD Pipeline

```mermaid
graph LR
    Push["git push / PR"] --> Compile["Compile"]
    Compile --> Spotless["Spotless Check<br/>(Formatting)"]
    Spotless --> Tests["Unit Tests<br/>+ ArchUnit"]
    Tests --> SpotBugs["SpotBugs<br/>(Static Analysis)"]
    SpotBugs --> JaCoCo["JaCoCo<br/>(Coverage)"]
    JaCoCo --> OWASP["OWASP<br/>Dependency Check"]

    style Push fill:#fff3e0
    style Tests fill:#e3f2fd
    style SpotBugs fill:#fce4ec
    style OWASP fill:#ffebee
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

## Database
- PostgreSQL 16 with Flyway migrations
- Schema managed in `src/main/resources/db/migration/`
- HikariCP connection pool (Spring Boot default)

## Quality Gates
- **Spotless** — Google Java Style (AOSP variant), enforced in CI
- **ArchUnit** — DDD layer rules, naming conventions, annotation checks
- **JaCoCo** — Minimum 60% line coverage
- **SpotBugs** — Static analysis, fail on Medium+ findings
- **OWASP Dependency Check** — CVE scanning on PRs
