# Path Manager (Fahrplanmanager) — User Guide

## Overview

The Path Manager simulates the TTT (Train Timetable Transfer) communication between a Responsible Applicant (RA) and an Infrastructure Manager (IM) within the order management system. It allows users to:

- Submit timetable positions as reference trains
- Walk through the full TTT process lifecycle step by step
- Compare train versions created by IM offers
- Edit train header (Zugkopf) and journey location (OP) attributes

The Path Manager is accessible from the sidebar navigation under "Fahrplanmanager" or "Path Manager".

## Navigation

### Sidebar Entry

The Path Manager view is listed in the `MainLayout` sidebar. Click **Fahrplanmanager** to navigate to the Path Manager overview at `/path-manager`.

### From Order Detail

Each `FAHRPLAN` position in the order detail view shows a train icon button:

- **Orange train icon** — "Send to Path Manager": The position has not been sent yet. Click to create a new reference train.
- **Teal train icon** — "View in Path Manager": The position was already sent. Click to navigate to the Path Manager and highlight the corresponding train.

## TreeGrid Structure

The Path Manager view is organized as a hierarchical TreeGrid with four levels:

```
Timetable Year 2026 (2025-12-14 to 2026-12-12)
  +-- Reference Train SOB0-000042-01-2026 (OTN: IC 123, State: BOOKED)
  |     +-- Version 1 (INITIAL) — Initial v1
  |     |     +-- 1. Zuerich HB (dep 08:00, Origin)
  |     |     +-- 2. Baden (arr 08:15, dep 08:17, Intermediate)
  |     |     +-- 3. Basel SBB (arr 09:00, Destination)
  |     +-- Version 2 (MODIFICATION) — MODIFICATION v2
  |           +-- 1. Zuerich HB (dep 08:05, Origin)
  |           +-- 2. Baden (arr 08:20, dep 08:22, Intermediate)
  |           +-- 3. Basel SBB (arr 09:05, Destination)
  +-- Reference Train SOB0-000043-01-2026 (OTN: EC 456, State: CREATED)
        +-- Version 1 (INITIAL) — Initial v1
              +-- ...
```

### Level 1: Timetable Year

Groups all trains by their timetable year. A timetable year typically runs from mid-December to mid-December (e.g., 2025-12-14 to 2026-12-12 for the year 2026).

### Level 2: Reference Train

Shows the reference train with:
- TRID (composite identifier: Company-Core-Variant-Year)
- Operational Train Number (OTN)
- Current process state (color-coded badge)
- Train type, weight, length, max speed

### Level 3: Train Version

Each version is a snapshot of the train data at a specific point in the lifecycle:
- **INITIAL** — Created when the train is first submitted
- **MODIFICATION** — Created by IM Draft Offer or Final Offer
- **ALTERATION** — Created by IM Alteration Offer after booking

### Level 4: Journey Location

Individual stops along the route with:
- Sequence number
- Location name and UOPID
- Arrival/departure times and qualifiers
- Dwell time
- Activity codes
- Journey location type (Origin, Intermediate, Destination, Handover, etc.)

## Submitting a Train from Order Management

### Prerequisites

1. The order position must be of type `FAHRPLAN`
2. The timetable must be saved (the Timetable Builder must have been completed at least once)
3. The position must not have been sent already

### Steps

1. Open the order detail view (`/orders/{orderId}`)
2. Find the `FAHRPLAN` position you want to submit
3. Click the **orange train icon** in the position row actions
4. The system creates a reference train and shows a success notification
5. The train icon turns **teal** — click it to navigate to the Path Manager

### What happens behind the scenes

The system calls `PathManagerService.createTrainFromOrderPosition()`, which:

1. Reads the saved `TimetableArchive` and its row data
2. Resolves or creates the appropriate `PmTimetableYear`
3. Creates a `PmReferenceTrain` with:
   - Generated TRID (company code, sequential core number, variant "01")
   - OTN from the order position
   - Train header fields (type, weight, length, speed) if available
   - Calendar data from the position's validity
4. Creates a `PmRoute` from the archive's route points
5. Creates an initial `PmTrainVersion` (v1, INITIAL) with all journey locations mapped from the timetable rows
6. Stores the reference train ID on the order position

## Simulating TTT Process Steps

The Path Manager allows you to walk through the entire TTT lifecycle by executing process steps one at a time. This simulates both RA (applicant) and IM (infrastructure manager) actions.

### How to Execute a Process Step

1. Select a reference train in the TreeGrid
2. The available actions for the current state are shown as buttons
3. Click an action button
4. Optionally add a comment
5. Confirm the step

### Example Flow: From Submission to Booking

Here is a typical happy-path flow through the TTT lifecycle:

| Step | Action | Role | New State | Creates Version? |
|---|---|---|---|---|
| 1 | SEND_REQUEST | RA | CREATED | No |
| 2 | IM_RECEIPT | IM | RECEIPT_CONFIRMED | No |
| 3 | IM_DRAFT_OFFER | IM | DRAFT_OFFERED | Yes (v2 MODIFICATION) |
| 4 | IM_FINAL_OFFER | IM | FINAL_OFFERED | Yes (v3 MODIFICATION) |
| 5 | ACCEPT_OFFER | RA | BOOKED | No |

### Example Flow: Rejection and Revision

| Step | Action | Role | New State | Creates Version? |
|---|---|---|---|---|
| 1 | SEND_REQUEST | RA | CREATED | No |
| 2 | IM_RECEIPT | IM | RECEIPT_CONFIRMED | No |
| 3 | IM_DRAFT_OFFER | IM | DRAFT_OFFERED | Yes (v2) |
| 4 | REJECT_WITH_REVISION | RA | REVISION_REQUESTED | No |
| 5 | IM_DRAFT_OFFER | IM | DRAFT_OFFERED | Yes (v3) |
| 6 | IM_FINAL_OFFER | IM | FINAL_OFFERED | Yes (v4) |
| 7 | ACCEPT_OFFER | RA | BOOKED | No |

### Example Flow: Post-Booking Alteration

| Step | Action | Role | New State | Creates Version? |
|---|---|---|---|---|
| (Starting from BOOKED state) | | | | |
| 1 | IM_ANNOUNCE_ALTERATION | IM | ALTERATION_ANNOUNCED | No |
| 2 | IM_ALTERATION_OFFER | IM | ALTERATION_OFFERED | Yes (ALTERATION) |
| 3 | ACCEPT_ALTERATION | RA | BOOKED | No |

### Process History

Each executed step is logged as an immutable `PmProcessStep` record. View the history by expanding the train detail or using the API:

```
GET /api/v1/pathmanager/process/{referenceTrainId}/history
```

## Editing Zugkopf (Train Header) Attributes

Train header attributes can be edited via the UI or the API. These are the fields on the `PmReferenceTrain`:

| Field | Description | Example |
|---|---|---|
| Operational Train Number | Betriebliche Zugnummer (OTN) | "IC 123", "95345" |
| Train Type | Zugart (2-character code) | "IC", "EC", "RE" |
| Traffic Type Code | Verkehrsart | Depends on network |
| Train Weight | Zuggewicht in Tonnen | 450 |
| Train Length | Zuglaenge in Metern | 200 |
| Max Speed | Hoechstgeschwindigkeit in km/h | 160 |
| Calendar Start/End | Verkehrstage-Bereich | 2026-01-05 to 2026-06-30 |
| Calendar Bitmap | Bitmap der Verkehrstage | "1111100..." (Mon-Fri) |

### API

```
PUT /api/v1/pathmanager/trains/{trainId}
Content-Type: application/json

{
  "operationalTrainNumber": "IC 124",
  "trainType": "IC",
  "trafficTypeCode": "0800"
}
```

## Editing Journey Location (OP) Attributes

Individual journey locations within a train version can be edited:

| Field | Description | Example |
|---|---|---|
| Arrival Time | Ankunftszeit (HH:mm or HH:mm:ss) | "08:15" |
| Departure Time | Abfahrtszeit | "08:17" |
| Dwell Time | Haltezeit in Minuten | 2 |
| Arrival Qualifier | TTT TimingQualifierCode | "ALA", "ELA", "PLA" |
| Departure Qualifier | TTT TimingQualifierCode | "ALD", "ELD", "PLD" |
| Activity Codes | Comma-separated TTT activity codes | "0001,0012" |

### API

```
PUT /api/v1/pathmanager/trains/{trainId}/versions/{versionId}/locations/{locationId}
Content-Type: application/json

{
  "arrivalTime": "08:20",
  "departureTime": "08:22",
  "dwellTime": 2,
  "arrivalQualifier": "ALA",
  "departureQualifier": "ALD",
  "activityCodes": ["0001"]
}
```

## Version Diff Feature

The diff feature compares order-side timetable data with the latest PM train version. This is useful after an IM offer to see what changed compared to the original request.

### How to Use

1. Select a reference train that has multiple versions
2. Click the "Diff" button
3. The system compares the order-side data (from TimetableArchive) with the latest PM version
4. Changes are highlighted: added locations, removed locations, and modified fields

### API

```
POST /api/v1/pathmanager/diff?referenceTrainId={uuid}
Content-Type: application/json

[
  {
    "sequence": 1,
    "primaryLocationName": "Zuerich HB",
    "uopid": "CH00001",
    "departureTime": "08:00",
    "journeyLocationType": "01"
  }
]
```

The response contains a structured diff result listing additions, removals, and modifications at the row and field level.

## REST API Reference

All Path Manager endpoints are documented via Swagger UI. Access the interactive documentation at:

```
http://localhost:8085/swagger-ui/index.html
```

Look for the tags:
- **Path Manager** — Train CRUD operations (7 endpoints)
- **TTT Process Simulation** — State machine operations (3 endpoints)
- **Timetable Diff** — Version comparison (1 endpoint)

For the complete endpoint listing with request/response schemas, see the [Architecture documentation](../ARCHITECTURE.md) or the [data model documentation](../datenmodel.md#path-manager-rest-api).
