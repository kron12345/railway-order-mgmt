# Architecture Overview

## System Context

```mermaid
graph LR
    Browser["Browser"] -->|HTTPS| App["Vaadin Flow App<br/>:8085"]
    App -->|JDBC| DB["PostgreSQL<br/>:5432"]
    App -->|OIDC| KC["Keycloak<br/>:8180"]
    App -->|WebSocket| Browser
    Browser -->|Tile / Map Requests| OSM["OpenStreetMap / Leaflet"]
```

## Layer Architecture

```mermaid
graph TB
    subgraph UI["UI Layer"]
        Layout["MainLayout"]
        Views["Views<br/>(Dashboard, Orders, Settings, Timetable Builder)"]
        Components["Reusable Components<br/>(StatusBadge, PositionTile, TimetableMap, ValidityCalendar)"]
    end

    subgraph Domain["Domain Layer (DDD)"]
        Order["Order Context<br/>orders / positions / resources / purchases"]
        Timetable["Timetable Context<br/>routing / archive / timetable rows"]
        InfraDomain["Infrastructure Context<br/>operational points / sections / tags / imports"]
        Customer["Customer Context"]
        Business["Business Context"]
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
    S-->>B: Redirect to Keycloak
    B->>K: Login
    K-->>B: Authorization Code
    B->>V: Callback with code
    V->>K: Exchange code for tokens
    K-->>V: ID Token + Access Token
    V->>S: Map Keycloak roles to GrantedAuthorities
    S-->>V: Authenticated session
    V-->>B: Render Vaadin UI
```

## Order Position Architecture

Order positions are stored in a single table (`order_positions`) and distinguished by `PositionType`.

- `LEISTUNG` uses a modal dialog editor and persists its business data directly on `order_positions`
- `FAHRPLAN` uses a dedicated full-screen builder and persists its detailed timetable in `timetable_archives`
- Both types share the same overview, detail, tagging, status, audit, and purchase-calendar presentation

```mermaid
flowchart LR
    O[OrderDetailView] --> P[OrderPositionPanel]
    P --> SD[ServicePositionDialog]
    P --> TB[TimetableBuilderView]

    SD --> OP[(order_positions)]

    TB --> TRS[TimetableRoutingService]
    TRS --> OPS[(operational_points)]
    TRS --> SOL[(sections_of_line)]
    TB --> TAS[TimetableArchiveService]
    TAS --> TA[(timetable_archives)]
    TAS --> OP
    TAS --> RN[(resource_needs)]
```

## Timetable Builder Flow

```mermaid
sequenceDiagram
    participant U as User
    participant TB as TimetableBuilderView
    participant RT as TimetableRoutingService
    participant DB as PostgreSQL
    participant AS as TimetableArchiveService

    U->>TB: Define from / via / to + anchor time
    TB->>RT: calculateRoute()
    RT->>DB: Read operational_points + sections_of_line
    DB-->>RT: Graph data
    RT-->>TB: Route + estimated timetable rows
    U->>TB: Refine row data, validity, halt/activity, comments
    TB->>AS: saveTimetablePosition()
    AS->>DB: Upsert timetable_archives
    AS->>DB: Upsert order_positions
    AS->>DB: Ensure CAPACITY resource_need with linked_fahrplan_id
    AS-->>TB: Saved position
```

## Live Updates (Push)

```mermaid
sequenceDiagram
    participant U1 as User A
    participant V1 as UI A
    participant BS as BroadcastService
    participant V2 as UI B

    U1->>V1: Change order data
    V1->>BS: broadcast(...)
    BS->>V2: notify listener
    V2->>V2: ui.access(refresh)
```

## Bounded Contexts

```mermaid
graph LR
    subgraph Order["Order Context"]
        O_M["Order<br/>OrderPosition<br/>ResourceNeed<br/>PurchasePosition"]
        O_R["Repositories"]
        O_S["OrderService"]
    end

    subgraph Timetable["Timetable Context"]
        T_M["TimetableArchive<br/>TimetableRowData"]
        T_R["TimetableArchiveRepository"]
        T_S["Routing + Archive Services"]
    end

    subgraph InfraDomain["Infrastructure Context"]
        I_M["OperationalPoint<br/>SectionOfLine<br/>PredefinedTag<br/>ImportLog"]
        I_R["Repositories"]
        I_S["CSV Import Services"]
    end

    subgraph Customer["Customer Context"]
        C_M["Customer"]
    end

    subgraph Business["Business Context"]
        B_M["Business"]
    end

    Order -.->|references| Customer
    Order -.->|optional links| Business
    Order -.->|capacity link| Timetable
    Timetable -.->|routes over| InfraDomain
```

## Layers

### UI Layer (`ui/`)

- Vaadin Flow server-side UI
- `layout/`: `MainLayout`, navigation, breadcrumbs, profile/theme context
- `view/`: route-annotated views such as `OrderListView`, `OrderDetailView`, `SettingsView`, `TimetableBuilderView`
- `component/`: reusable UI building blocks such as `PositionTile`, `OrderPositionRow`, `PurchaseCalendarPanel`, `TimetableMap`
- Styling: custom theme in `frontend/themes/order-mgmt/` plus Vaadin Lumo primitives

### Domain Layer (`domain/`)

- Organized by bounded context
- `order/`: orders, positions, resource needs, purchase positions, status model
- `timetable/`: route search, timetable archive, TTT-like row model
- `infrastructure/`: operational points, sections of line, tag catalog, import logs
- `customer/`, `business/`: supporting master/business data

### Infrastructure Layer (`infrastructure/`)

- `security/`: Spring Security + Keycloak OIDC
- `i18n/`: translation provider for DE/EN/IT/FR
- `push/`: broadcast service for UI refresh
- `config/`: application-level configuration

## Database

- PostgreSQL 16 with Flyway migrations `V1` to `V7`
- Shared order-position table with typed behavior via `PositionType`
- `timetable_archives` stores the detailed timetable rows as `jsonb`
- `resource_needs.linked_fahrplan_id` provides the technical link from a `CAPACITY` need to an archived timetable
- Hibernate Envers tracks audited entities in dedicated `_audit` tables

## Quality Gates

- **Spotless**: formatting
- **ArchUnit**: DDD layer rules and conventions
- **JaCoCo**: coverage
- **SpotBugs**: static analysis
- **OWASP Dependency Check**: dependency CVE scanning
- **Playwright**: browser-based regression paths, including the timetable builder
