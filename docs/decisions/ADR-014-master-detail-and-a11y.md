# ADR-014: Bloomberg-Style Master-Detail Layout, Keyboard-First UX, A11y Baseline

## Status
Accepted (2026-05-04)

## Context

`/orders` and `/businesses` had inconsistent UI patterns:

- `/orders`: accordion list (`OrderAccordionRow`) inside `OrderListView`; clicking
  a position-tile inside the accordion navigated to the parent order, not to the
  individual position. Users reported this as confusing.
- `/businesses`: flat data grid (`BusinessListView`); selecting a row navigated
  to `BusinessDetailView` as a separate page. Visually inconsistent with orders.
- No global way to jump quickly between domains; no command palette; no
  keyboard-first affordances; status was signalled only by colour (WCAG fail).
- Accessing a single `OrderPosition` always went through the order aggregate.

## Decision

### 1. Unified master-detail shell

Both `/orders` and `/businesses` use the same generic shell
`MasterDetailLayout<T>` (in `ui/component/masterdetail/`):

- Left: scrollable card list (28 % split), keyboard-focusable
  (`role="listbox"`, ARIA `aria-selected`).
- Right: detail panel — replaces content based on URL, never opens a new page.
- Toolbar: filter (`/`), context-specific actions (`+ neu` on `n`).

URL drives selection. Routes:

| Route | View | Detail content |
| --- | --- | --- |
| `/businesses` | `BusinessOverviewView` | empty state |
| `/businesses/{id}` | same | `BusinessDetailView` (embedded) |
| `/businesses/new` | same | empty business form |
| `/orders` | `OrderOverviewView` | empty state |
| `/orders/{orderId}` | same | `OrderDetailView` (embedded) |
| `/orders/{orderId}/positions/{posId}` | same | `OrderPositionDetailView` |

Old top-level routes `BusinessListView` and `OrderListView` are deleted.
`BusinessDetailView` and `OrderDetailView` lose their `@Route`; they become
embeddable components. `OrderDetailView` is `@SpringComponent` with prototype
scope (11 service deps; one `ObjectProvider` injection in
`OrderOverviewView` produces fresh instances per navigation).

Cross-order deep links (`/orders/{A}/positions/{B}` where B belongs to a
different order) are rejected and redirect to the correct order.

### 2. Keyboard-first

Global shortcuts (registered in `MainLayout` via a body-level `keydown`
listener guarded against typing in inputs):

- `g o`, `g b`, `g h` — jump to Orders / Businesses / Home (vim-style two-key)
- `Ctrl+K` — open `CommandPalette` (fuzzy across orders, businesses,
  positions; arrow keys navigate, Enter activates)
- `Shift+?` — open `KeyboardHelpOverlay` (full shortcut reference)

Per-list shortcuts (registered on `MasterDetailLayout`):

- `/` — focus filter
- `↑ ↓ Home End` — navigate cards, updates URL + announces via aria-live
- `Enter` — activate
- `Esc` — clear filter
- `n` — new entry

### 3. A11y baseline

New helper components in `ui/component/a11y/`:

- `SkipLinks` — visible-on-focus "skip to filter / list / detail" anchors.
- `BreadcrumbBar` — pinned `<nav aria-label="breadcrumb">` with
  `aria-current="page"` on the last crumb (WAI-ARIA breadcrumb pattern).
- `AriaLive` — visually hidden `role="status"` `aria-live="polite"` region
  used by `MasterDetailLayout` to announce selection changes.

Status indicators no longer rely on colour alone: status pills include an
icon + uppercase text label (`⏳ In Bearbeitung`, `✓ Freigegeben` etc.),
and a 4 px coloured gutter on each card adds redundant signalling.

### 4. Generic per-user view preferences

New domain `domain/userprefs/` with table `user_view_preferences`
(V30 migration): `(user_id, view_key) -> JSON payload`. Reused by
`GridPreferenceBinder<T>` for column order / width / visibility on any
Vaadin Grid; designed generically so future views (filter state,
splitter positions) can persist into the same table.

## Consequences

### Positive

- Identical interaction model in `/orders` and `/businesses` (consistency).
- One-click access to any single position — no more "going through the
  parent order" surprise.
- Power users can stay on the keyboard from search to detail to next entry
  without touching the mouse.
- Skip-links and breadcrumbs satisfy WCAG 2.4.1 (Bypass Blocks) and 2.4.8
  (Location). ARIA listbox + live-region for screen readers.
- `MasterDetailLayout` is reused twice already and stays small (<300 lines),
  so future domains (e.g. fleet, customers) can adopt the same pattern.

### Negative / accepted trade-offs

- Adding a master-detail shell on top of an existing detail view adds one
  layer of indirection; mitigated by the fact that the embeddable detail
  views are the only callers.
- The accordion's "peek without navigating" affordance is gone; users now
  see the same data on the right pane without leaving the overview, so the
  use case is preserved.
- Two-key shortcuts (`g o`) require a brief mental model adjustment.

## Notes

- `OrderDetailView` internal structure (compact header + positions panel)
  is *unchanged* in this ADR; a future ADR may refactor it into a TabSheet
  (Stammdaten / Positionen / Bestellpositionen / Geschäfte / Historie).
- A separate Codex review found three pre-existing P1/P2 bugs in
  `PathManagerService` and `VehiclePlanningView`; tracked outside this
  scope.
