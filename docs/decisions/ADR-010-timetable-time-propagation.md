# ADR-010: Timetable Time Propagation, Modes, and Editing Rules

## Status
Accepted (revised — original SHIFT/STRETCH ADR extended with mode model, endpoint rules, half-windows, day offset, and TTT export gating)

## Context
When editing arrival or departure times in a timetable, changes at one stop often need to propagate to subsequent stops, and the underlying TTT TimingQualifier model offers more nuance than a single "exact time" can express. The editor must:

1. Let the user enter exact, window, or commercial times — or only a lower/upper bound (TTT half-window).
2. Propagate edits through neighbouring rows without forcing the user to recompute downstream times by hand.
3. Distinguish data the user has explicitly entered (which gets exported via TTT) from interpolated routing estimates.
4. Respect TTT's "Mindesthaltezeit" (DwellTime) semantic and the asymmetry between origin/destination/intermediate halts.

## Decision

### 1. Time-constraint modes per row, per side

`TimeConstraintMode` enum (six values), applied independently to arrival and departure side:

| Mode | TTT TimingQualifierCode(s) | UI input | Bedeutung |
|---|---|---|---|
| `NONE` | – (no Timing entry exported) | – | only routing estimate, not exported |
| `EXACT` | ALA / ALD | one time | exact time required |
| `WINDOW` | ELA + LLA / ELD + LLD | two times | both bounds, full TTT window |
| `AFTER` | ELA / ELD | one time | half-window: "no earlier than", `≥` |
| `BEFORE` | LLA / LLD | one time | half-window: "no later than", `≤` |
| `COMMERCIAL` | PLA / PLD | one time | published timetable time |

`AFTER` and `BEFORE` are TTT-valid because `TimingAtLocation` has `Timing (0..*)` — a single `Timing` entry with the appropriate qualifier code is permitted, ELA/LLA do not have to occur in pairs.

### 2. Anchors and propagation

Each row has two **effective anchors** computed from its current mode:

- `effectiveArrivalAnchor(row)` — used for **backward** propagation (preceding rows). Returns ELA for WINDOW/AFTER, LLA for BEFORE, ALA for EXACT, PLA for COMMERCIAL, or `estimatedArrival` for NONE.
- `effectiveDepartureAnchor(row)` — used for **forward** propagation. Returns LLD for WINDOW/BEFORE (max-time anchor), ELD for AFTER (only available bound), ALD for EXACT, PLD for COMMERCIAL, or `estimatedDeparture` for NONE.

Propagation modes (`TimePropagationMode`):

- **SHIFT**: every neighbour row's time fields (estimated, exact, ELA/LLA or ELD/LLD, commercial) are translated by the delta between old and new anchor. Stops at the first pinned row.
- **STRETCH**: times between the changed row and the next/previous pinned row are scaled proportionally so the schedule still meets the pin.

Default is STRETCH; user can switch per change.

#### Forward fallback (Regel 7)
Forward STRETCH requires a real anchor ahead — either an explicit pin or a user-entered arrival on the destination. Without one, the implementation falls back to SHIFT to avoid stretching against thin air. Backward STRETCH always works (origin acts as implicit pin).

#### Day offset
`arrivalOffset` and `departureOffset` per row encode TTT `Offset` values (e.g. +1 for next-day, -1 for previous-day modifications). Propagation does arithmetic in absolute minutes (`offsetDays * 1440 + minute`) so a 23:50 → +30 min shift correctly produces 00:20 with offset +1.

### 3. Pin semantics

A row may be marked `pinned = true`:
- In SHIFT mode, propagation stops at the next pin.
- In STRETCH mode, the pin is the stretch endpoint.
- Pin works on halt rows AND pass-through rows (a pinned pass-through freezes its interpolated time).

Origin/destination act as implicit boundaries even without an explicit pin — origin pins backward stretches, destination pins forward stretches when the user has entered an arrival anchor there.

### 4. Editing rules (Regeln 1–7)

The editor enforces a small rule set in `TimetableEditingService.applyHaltRules(row)`:

1. **Halt off → no time inputs.** All constraint fields and the dwell are cleared. Estimated times remain (used for interpolation only).
2. **Pass-through stops** can still appear in TTT (`JourneyLocationTypeCode` 04) without `TimingAtLocation`. Marked `manuallyAdded` if the user added them; auto-routed waypoints are not exported.
3. **Mutual exclusion**: a halt row may have either dwell + one side, or both sides without dwell. All three together is forbidden.
4. **Same-mode mirroring**: when dwell + one side is set, the other side is auto-derived in the same mode (EXACT→EXACT, WINDOW→WINDOW with both bounds shifted, AFTER→AFTER, BEFORE→BEFORE, COMMERCIAL→COMMERCIAL).
5. **Origin / destination special-cases**: implicit halts, no halt-checkbox, no dwell, only one side allowed (origin → departure-only, destination → arrival-only).
6. **Backward propagation** triggers when the arrival anchor changes; uses the earliest meaningful arrival (ELA for WINDOW, LLA for BEFORE, ALA for EXACT). Origin acts as an implicit stretch endpoint.
7. **Forward propagation** triggers when the departure anchor changes; uses the latest meaningful departure (LLD for WINDOW/BEFORE, ELD for AFTER, ALD for EXACT). Falls back to SHIFT when no destination arrival or pin exists.

### 5. Speed interpolation between anchors

`interpolateBetweenAnchors(rows)` fills `estimatedArrival`/`estimatedDeparture` on every row that lacks user-entered times by:
- Collecting anchors from rows where the user actually entered time data (`hasIntentArrival/Departure`, which also covers dwell-mirrored values).
- For each pair of consecutive anchors, computing segment speed = `Δtime / Δdistance` and applying it to intermediate rows in proportion to their `distanceFromStartMeters`.
- Outside the anchor range (rows before the first or after the last), default to **70 km/h**.

Result: a single anchor anywhere on the route is enough to produce sensible estimated times for the entire path.

### 6. TTT export gating (`userEntered*` flags)

Each constraint field has a corresponding `userEntered*` boolean on `TimetableRowData`:

```
userEnteredArrivalExact, userEnteredArrivalEarliest, userEnteredArrivalLatest, userEnteredCommercialArrival,
userEnteredDepartureExact, userEnteredDepartureEarliest, userEnteredDepartureLatest, userEnteredCommercialDeparture,
userEnteredDwell
```

Only fields with `userEntered* = true` are exported as TTT `Timing` entries. Mirrored/derived/propagated values stay local. The grid shows a "TTT" badge in accent color on every column that will be exported (point name for halts/origin/destination/manually-added rows, time cells for user-entered values, dwell for `userEnteredDwell`).

### 7. Mode-switch preservation

When the user switches a side's mode (e.g. EXACT → WINDOW), `preserveOnModeSwitch` copies the prior single value into the new fields (e.g. EXACT 08:00 becomes WINDOW ELA=LLA=08:00). No data is silently lost.

## Rationale
- **Six modes** cover all single-Timing TTT cases — no "almost" cases force the user to misuse WINDOW.
- **Pins + asymmetric STRETCH/SHIFT fallback** keep edits predictable: STRETCH never compresses a schedule against an undefined endpoint.
- **userEntered flags as TTT export gate** are simpler than tracking constraint origin (mirrored vs typed); the editor sets them per keystroke.
- **Segmental interpolation** lets the user enter as little or as much as they want — even one anchor on a 30-stop route gives realistic intermediate times.
- **Day offset** is a thin layer on top of LocalTime; full datetime arithmetic happens only inside the propagation helpers.

## Consequences
- `TimetableRowData` carries 9 user-entered booleans + 2 day offsets in addition to the existing constraint fields. Persisted in the archive JSON.
- `TimetableEditingService` is the single source of truth for time arithmetic and rule enforcement (≈ 30 public/private methods, all unit-tested).
- `TimetableRowEditorPanel` is rule-driven: section visibility derives from `halt`/`isOrigin`/`isDestination` and mode-specific field visibility from the selected mode.
- The grid uses `ComponentRenderer` for time/point/dwell columns to support the TTT-badge styling.
- Future extensions (mid-night offset on the WINDOW level, multi-day services, time-zone-aware times) can be layered on top without changing the public service API.

## Related
- ADR-009: Fahrplan archive link
- ADR-011: Path Manager architecture (same TimingQualifier mapping target)
- TTT/TSI TAF/TAP Anlage 1 §3.6, §5.7 (TimingQualifierCode catalog)
