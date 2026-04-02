# Timetable Builder — User Guide

## Overview

The Timetable Builder is a full-screen editor for creating and editing timetable positions (`FAHRPLAN`). It follows a two-step workflow:

1. **Route Step** — Define the journey route (from, via, to) and anchor time
2. **Table Step** — Refine the complete timetable: edit times, add/remove stops, set activities and validity

The builder is accessed from an order's detail view by creating or editing a `FAHRPLAN` position. The URL pattern is `/orders/{orderId}/timetable-builder`.

![Step 1 Route](screenshots/timetable-step1-route.png)

## Step 1: Route Definition

In the first step, you define the geographical route of the train journey:

- **Name**: A mandatory name for the position (e.g., "IC 123 Zuerich - Basel")
- **From / To**: Mandatory origin and destination, selected from the operational points database (ERA RINF)
- **Via**: Optional ordered intermediate waypoints that the route must pass through
- **Anchor Time**: Either the departure time at the origin or the arrival time at the destination
- **Tags**: Optional classification tags from the `POSITION` and `GENERAL` catalogs
- **Comment**: Optional free-text comment

### Operational Train Number (OTN)

An optional free-text field for the operational train number (max 20 characters). Since OTN is not strictly numeric, it also supports placeholder patterns such as `95345` or `95xxx`.

When set, the OTN appears:

- On **position tiles** in the order list view
- On **position rows** in the order detail view
- In the timetable metadata accordion in the builder itself

The OTN maps to the TTT `OperationalTrainNumber` field for future export.

When you click "Calculate Route", the system finds the shortest path through the infrastructure graph (`sections_of_line`) and estimates arrival/departure times at every operational point along the route based on a default speed assumption (70 km/h).

A Leaflet/OpenStreetMap panel shows the route with straight lines between operational point coordinates.

![Step 1 Map](screenshots/timetable-step1-map.png)

## Step 2: Timetable Editing

The second step shows the **complete calculated route** as an editable table. Every operational point along the route appears as a row — not just the from/via/to points from Step 1.

![Step 2 Table](screenshots/timetable-step2-table.png)

### Grid Columns

| Column | Description |
|---|---|
| Sequence | Row number |
| Operational Point | Name and UOPID |
| Role | `ORIGIN`, `VIA`, `DESTINATION`, or `AUTO` (auto-calculated) |
| From / To | Context showing previous and next operational point |
| Distance | Cumulative distance from start in km |
| Est. Arrival / Departure | Automatically estimated times from routing |
| Halt | Whether the train stops at this point |
| Activity | Mandatory when halt is set — the reason for stopping |
| Arrival / Departure Constraint | Time mode: NONE, EXACT, WINDOW, or COMMERCIAL |

### Selecting and Editing a Row

Click any row in the grid to open the **Row Editor Panel** on the right side. The editor shows:

- Operational point name, role, and context (from/to neighbors)
- Estimated arrival and departure times
- Halt checkbox and dwell time (minutes)
- Activity selection (mandatory when halt is checked)
- Arrival and departure time modes with corresponding input fields
- Pin toggle for time propagation control
- Propagation mode selector (Shift / Stretch)

![Step 2 Editor](screenshots/timetable-step2-editor.png)

## Adding and Removing Stops

### Adding a Stop

1. Click the **+** button on any row in the grid
2. An inline form appears below the grid
3. Select an operational point from the dropdown
4. Select a TTT activity code (mandatory for new stops)
5. Click "Add"

The new stop is inserted after the selected row. Times are interpolated from the neighboring rows, and a default dwell time of 2 minutes is assigned. The stop is marked as `manuallyAdded` to distinguish it from route-calculated points.

### Removing a Stop

1. Click the **delete** button on a row
2. The row is **soft-deleted**: it appears with a strike-through and is excluded from time calculations
3. Click the delete button again to **undo** the soft-delete
4. Soft-deleted rows are permanently removed when the timetable is saved

Origin and destination rows cannot be deleted.

## Time Editing

### Time Modes (TimeConstraintMode)

Each arrival and departure can have one of four constraint modes:

| Mode | Meaning | Fields |
|---|---|---|
| **NONE** | No explicit time constraint. Only the estimated time from routing is shown. | — |
| **EXACT** | A single exact time. | One time picker |
| **WINDOW** | A time window with earliest and latest bounds. | Two time pickers (earliest + latest) |
| **COMMERCIAL** | A published/commercial timetable time. | One time picker |

### Shift and Stretch Propagation

When you change a time, you can control how that change affects downstream stops:

**Shift Mode** (default): All following stops are moved by the same amount. If you add 10 minutes to a departure, all subsequent arrivals and departures also gain 10 minutes — until a pinned stop is reached.

**Stretch Mode**: Times between the changed stop and the next pinned stop are proportionally redistributed. The overall time span changes, but the relative proportions of travel segments are preserved.

### Pinning Times

Click the **pin icon** on a row to mark it as pinned. Pinned rows act as anchors:

- In **Shift** mode, propagation stops at the next pin
- In **Stretch** mode, the pin defines the far boundary of the stretch interval

Use pins on important fixed-time stops such as border crossings, interchange connections, or published commercial times.

### Relative Time Input

In time fields, you can type relative offsets:

- `+5` adds 5 minutes to the current estimated time
- `-3` subtracts 3 minutes from the current estimated time

The system resolves the offset against the estimated time of the current row.

## TTT Activity Codes

Activity codes describe why a train stops at a particular point. They follow the TTT (Train Timetable Transfer) standard. A selection of commonly used codes:

| Code | Description |
|---|---|
| 0001 | Ein- und Aussteigen / Ein- und Ausladen |
| 0002 | IM Betrieblicher Halt |
| 0003 | Diensthalt |
| 0004 | Wechsel von Leitsystemen |
| 0005 | Wenden ohne Triebfahrzeugwechsel |
| 0006 | Wenden mit Triebfahrzeugwechsel |
| 0012 | Tfz-Wechsel |
| 0013 | Anhaengen Wagen |
| 0014 | Abhaengen Wagen |
| 0023 | Lokfuehrerwechsel |
| 0028 | Nur Einsteigen |
| 0029 | Nur Aussteigen |
| 0030 | Halt auf Verlangen |
| 0040 | Durchfahrt |
| CH08 | Durchfahrt mit berechneter Mindesthaltezeit |
| CH09 | Durchfahrt ohne berechnete Mindesthaltezeit |

The full catalog contains 35+ codes including Swiss-specific extensions (CH08-CH17). Activity selection is mandatory when a row has `halt = true`.

## TTT TimingQualifier Codes

The system maps its internal `TimeConstraintMode` to TTT TimingQualifierCodes for future export:

| Internal Mode | Arrival Codes | Departure Codes | Meaning |
|---|---|---|---|
| NONE | — | — | No timing qualifier exported |
| EXACT | **ALA** | **ALD** | Actual/Agreed Latest Arrival/Departure |
| WINDOW | **ELA** + **LLA** | **ELD** + **LLD** | Earliest + Latest Arrival/Departure |
| COMMERCIAL | **PLA** | **PLD** | Published (commercial) Arrival/Departure |

**ALA / ALD** — The effective agreed time. Used for stops where the exact time is contractually fixed.

**ELA / LLA** — Earliest and Latest Arrival. Defines a window within which the train may arrive. The corresponding departure codes are ELD / LLD.

**PLA / PLD** — Published/commercial timetable time. This is the time shown in public timetables and used for passenger information. It may differ from the operational time.

## Validity

At the bottom of the Table Step, a multi-date calendar allows selecting which days within the order's validity period this timetable applies. The validity is stored as date segments on the order position.

![Step 2 Validity](screenshots/timetable-step2-validity.png)

## Fahrplan-Detailansicht (Archive View)

After a timetable position has been saved, it can be viewed in a read-only detail view.

### Accessing the Archive View

From the order detail view, each `FAHRPLAN` position row shows an **eye icon** button. Click it to open the timetable archive view at the URL `/orders/{orderId}/timetable/{positionId}`.

### What it shows

The archive view provides a read-only presentation of the saved timetable:

- **Header**: Position name, OTN (Operational Train Number), route summary, status badge, and an "Edit" button
- **Left panel (65%)**: A color-coded timetable table displaying all route points with sequence number, name, arrival/departure times, dwell time, activity code, and TTT timing qualifiers. Origin/Destination rows are highlighted in amber, halt rows in teal, pass-through rows are muted, and soft-deleted rows appear with strikethrough
- **Right panel (35%)**: A sidebar with three cards:
  - **Map**: Leaflet/OpenStreetMap showing the route with markers and polylines
  - **Validity**: Date range and day count from the position's validity segments
  - **Metadata**: OTN, timetable type, route summary, created/updated timestamps

### Navigating to the Builder

The "Edit" button in the header navigates to the Timetable Builder (`/orders/{orderId}/timetable-builder?positionId={positionId}`) where the timetable can be modified.

![Archive View](screenshots/timetable-archive-view.png)

## Sending to Path Manager

After a timetable position has been saved, it can be sent to the **Path Manager** for TTT (Train Timetable Transfer) processing.

### How to Send

In the order detail view, each `FAHRPLAN` position row shows a **train icon** button next to the view/edit/delete actions:

- **Orange train icon** (tooltip "Send to Path Manager"): The position has not been sent yet. Click to create a reference train in the Path Manager from this position's timetable data.
- **Teal train icon** (tooltip "View in Path Manager"): The position has already been sent. Click to navigate to the Path Manager view.

### What Happens on Send

When you click "Send to Path Manager", the system:

1. Reads the saved timetable archive and its row data for the position
2. Calls `PathManagerService.createTrainFromOrderPosition()` which:
   - Resolves or creates the applicable timetable year based on the position's start date
   - Creates a `PmReferenceTrain` with a generated TRID (company, core, variant) and the position's OTN
   - Creates a `PmRoute` from the timetable row data
   - Creates an initial `PmTrainVersion` (v1) with all journey locations mapped from the timetable rows
3. Stores the reference train's ID on the order position (`pmReferenceTrainId`)
4. Shows a success notification

The reference train appears in the Path Manager view under the appropriate timetable year and can then go through the TTT process lifecycle (Send Request, IM Receipt, Draft/Final Offer, Accept, Book, etc.).

### Prerequisites

- The position must be of type `FAHRPLAN`
- The position must have a saved timetable archive (i.e., the timetable builder must have been completed and saved at least once)
- The position must not have been sent already (`pmReferenceTrainId` must be null)

## Saving

Click "Save" to persist the timetable. The system:

1. Stores the full timetable table as JSON in the `timetable_archives` table
2. Mirrors key metadata (from/to, start/end, tags, validity, OTN) onto the `order_positions` row
3. Creates or reuses a `CAPACITY` resource need linking the position to the archive

The builder navigates back to the order detail view after a successful save.
