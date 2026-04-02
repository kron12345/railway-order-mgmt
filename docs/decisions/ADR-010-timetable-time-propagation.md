# ADR-010: Timetable Time Propagation with Shift and Stretch Modes

## Status
Accepted

## Context
When editing arrival or departure times in a timetable, changes at one stop often need to propagate to subsequent stops. Without propagation, the user would need to manually adjust every downstream time after a single change, which is error-prone and tedious for timetables with dozens of stops.

There are two fundamentally different editing intentions:

1. **Move the entire schedule**: A train departs 15 minutes later, so all following times should shift by 15 minutes.
2. **Adjust within a fixed window**: The dwell time at a stop changes, but the train must still arrive at a specific downstream stop at the originally planned time.

Both patterns are common in railway timetable planning and correspond to different operational scenarios.

## Decision
We implement two propagation modes via the `TimePropagationMode` enum:

### SHIFT (default)
All following times are translated by the same delta (difference between old and new time). Propagation moves forward through the timetable and stops at the next **pinned** row. This preserves all relative travel and dwell times downstream.

### STRETCH
Times between the changed row and the next pinned row are scaled proportionally. If the available time span between two anchor points changes, all intermediate travel times are multiplied by the same ratio. This preserves the relative proportions of travel segments while fitting them into a new time window.

### Pin Concept
A row can be marked as `pinned = true`. Pinned rows serve as immovable anchors:
- In SHIFT mode, propagation stops at the next pin — rows at and beyond the pin are not affected.
- In STRETCH mode, the pin defines the far end of the stretch interval — intermediate times are scaled between the changed row and the pin.

Origin and destination rows cannot be deleted but can be pinned. Any intermediate row can be pinned (e.g., border crossings, commercial stops, interchange points).

## Rationale
- **Two modes cover the main editing patterns** observed in real timetable work: global schedule shift and local time redistribution.
- **Pins provide user control** over propagation boundaries without requiring complex constraint systems.
- **The model is simple to implement**: SHIFT is a constant addition, STRETCH is a linear interpolation. No solver or constraint engine is needed.
- **The approach is non-destructive**: original estimated times from routing remain in `estimatedArrival`/`estimatedDeparture`, while explicit times go into `arrivalExact`/`departureExact`. The user can always see the routing estimate for comparison.

## Consequences
- The `TimetableRowData` model gains a `pinned` field (persisted in the archive JSON).
- The `TimetableEditingService` centralizes all propagation logic in the domain layer, keeping UI components thin.
- The `TimetableRowEditorPanel` UI must clearly communicate which propagation mode is active and which rows are pinned, so the user understands the scope of a time change before confirming.
- Pinned rows are visually distinguished in the grid (pin icon) so that propagation boundaries are immediately visible.
- Future extensions (e.g., backward propagation, multi-pin stretch) can be added without changing the core model.
