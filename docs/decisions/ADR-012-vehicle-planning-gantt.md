# ADR-012: Vehicle Planning with Gantt Chart and Vaadin DnD

## Status
Accepted

## Context

The order management system needs a vehicle rotation planning feature (Umlaufplanung) that allows dispatchers to visually assign reference trains from the Path Manager to physical vehicles. The planning must support:

- Weekday-based rotation planning (Monday through Sunday)
- Visual overview of vehicle utilization across the day
- Drag-and-drop assignment of trains to vehicles
- Automatic conflict detection (time overlaps, location mismatches)
- Support for coupling/decoupling operations (Mehrfachtraktion)

Several visualization approaches were considered:

1. **Table-based UI** with manual time entry
2. **Third-party Gantt library** (e.g. DHTMLX Gantt, Bryntum)
3. **Custom Gantt with CSS Grid + Vaadin DnD**
4. **Timeline component from Vaadin Directory**

## Decision

We implement a **custom Div-based Gantt chart using CSS absolute positioning and Vaadin's built-in DragSource/DropTarget API**. The Vehicle Planning module calls Path Manager services directly (no REST API layer).

### Key design choices:

- **No REST API for VP**: Vehicle Planning operates entirely through the Vaadin Flow server-side UI layer. Since both VP and PM run in the same JVM, direct Spring service calls are simpler and sufficient. A REST API would add unnecessary serialization overhead for a planning tool that is inherently interactive.

- **Direct PM service dependency**: `VehiclePlanningService` and `ConflictDetectionService` query `PmReferenceTrain` and `PmJourneyLocation` entities directly to obtain train times and locations. This tight coupling is acceptable because VP is conceptually a downstream consumer of PM data.

- **CSS Grid Gantt instead of third-party library**: A custom Div-based approach avoids adding a heavy JavaScript dependency and integrates naturally with Vaadin's server-side rendering model. The Gantt blocks are `<div>` elements with `position: absolute`, width calculated proportionally from train departure/arrival times.

- **Vaadin DragSource/DropTarget**: Using Vaadin's built-in drag-and-drop API keeps the interaction server-side and avoids custom JavaScript interop.

- **No Envers auditing**: VP entities are planning data, not binding operational records. Auditing is omitted to keep the planning workflow lightweight.

## Consequences

### Positive
- Simple architecture: no additional API layer or JavaScript dependencies
- Fast iteration: changes to the Gantt visualization only require Java/CSS changes
- Conflict detection runs server-side with full access to PM data (journey locations, times)
- Drag-and-drop UX is intuitive for dispatchers

### Negative
- **Tight coupling to PM**: If the Path Manager were extracted into a separate microservice, VP would need significant refactoring
- **No external API**: Other systems cannot access VP data programmatically. If needed, a REST API would have to be added later.
- **Custom Gantt limitations**: The CSS-based Gantt lacks features of mature Gantt libraries (zooming, dependency arrows, resource leveling). These can be added incrementally if needed.
- **Server-side rendering**: Each drag-and-drop operation triggers a server round-trip. For very large rotation sets (100+ trains per day), latency could become noticeable.

## Alternatives Considered

| Alternative | Reason for Rejection |
|---|---|
| Table-based UI | Poor user experience for visual planning; hard to spot conflicts |
| DHTMLX / Bryntum Gantt | Heavy commercial JS libraries; complex Vaadin integration; license cost |
| Vaadin Timeline (Directory) | Limited maintenance; does not support DnD out of the box |
| Separate VP microservice with REST API | Over-engineering for a planning tool that shares the same database |
