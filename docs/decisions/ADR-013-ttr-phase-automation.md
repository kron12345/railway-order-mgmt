# ADR-013: TTR Phase Automation with TtrPhaseResolver

## Status
Accepted

## Context

The European TTR (Timetable Redesign) framework defines distinct phases for timetable path ordering. Each phase has different rules for the TTT (Train Timetable Transfer) process:

- **Annual Ordering (Bestellphase 2)**: Full process with draft offers allowed (ProcessType = ANNUAL_NEW, code 0)
- **Late Ordering (Bestellphase 3)**: Shortened process without draft offers (ProcessType = ANNUAL_LATE, code 1)
- **Ad Hoc Ordering**: Short-notice requests (ProcessType = AD_HOC, code 2)
- **Earlier phases** (Capacity Strategy, Model, Supply): No ordering possible yet

Previously, the ProcessType had to be manually selected or was not considered at all. This led to:
- Users accidentally submitting trains with the wrong ProcessType
- Draft offers being simulated in phases where they are not allowed
- No visual indication of which TTR phase a timetable year is in

## Decision

We implement a **stateless TtrPhaseResolver service** that automatically determines the current TTR phase from the timetable year's start date and today's date.

### Key design choices:

- **Date-based calculation, not stored state**: The phase is computed on every call from `PmTimetableYear.startDate` minus month offsets. There is no `current_phase` column in the database. This ensures the phase always reflects the current date, even when the application restarts or dates change overnight.

- **Month-based offsets from X**: Phase boundaries are defined as: X-60, X-36, X-18, X-11, X-8.5 (8 months + 15 days), X-2. These match the standard TTR timeline used by European infrastructure managers.

- **Integration into PathProcessEngine**: The engine calls `TtrPhaseResolver.isDraftOfferAllowed()` to dynamically filter the available actions. In Bestellphase 3 (Late Ordering), `IM_DRAFT_OFFER` is removed from the available actions set. Additionally, `RECEIPT_CONFIRMED → FINAL_OFFERED` via `IM_FINAL_OFFER` is enabled directly.

- **Multi-year seed data**: V14 migration seeds timetable years 2025 and 2027 (2026 already existed from V9) with `ON CONFLICT DO NOTHING`, allowing demonstration of different phases across multiple years.

- **UI feedback**: Color-coded badges in the PathManagerView and a phase info box in the ProcessSimulationPanel provide immediate visual feedback about ordering constraints.

## Consequences

### Positive
- **Automatic correctness**: ProcessType is always correct for the current date; no manual selection needed
- **Dynamic state machine**: Available actions adapt to the TTR phase without code changes per year
- **Testable**: TtrPhaseResolver is a pure function (year + date → phase), covered by 25 unit tests including boundary dates
- **Visual clarity**: Users immediately see which ordering phase applies and what constraints exist

### Negative
- **No override mechanism**: If a user needs to submit a train with a different ProcessType (e.g., testing scenarios), there is currently no manual override
- **Phase boundaries are hardcoded**: The month offsets (X-60, X-36, etc.) are compiled into the TtrPhaseResolver. If the TTR framework changes, a code change is needed. However, these boundaries are defined by EU regulation and change very rarely.
- **Different state machine paths per phase**: The combined behavior of `resolvePhase` + `isDraftOfferAllowed` + `PathProcessEngine` creates two implicit "process profiles" (with/without draft). This complexity is manageable now but could grow if more phase-specific rules are added.

## Alternatives Considered

| Alternative | Reason for Rejection |
|---|---|
| Manual ProcessType selection in UI | Error-prone, users would need to know the current TTR phase |
| Stored phase column on PmTimetableYear | Would require a background job or trigger to update; date calculation is simpler and always correct |
| Separate state machine per phase | Over-engineering; the current approach of filtering actions is simpler and covers the requirements |
| Configuration-based phase boundaries | Would add complexity for boundaries that rarely change (EU regulation); can be added later if needed |
