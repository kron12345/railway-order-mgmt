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

## TTR-Phasen und Bestellprozesse

### Automatische ProcessType-Zuordnung

Der Fahrplanmanager erkennt automatisch, in welcher TTR-Phase (Timetable Redesign) sich ein Fahrplanjahr befindet, und bestimmt daraus den TTT ProcessType fuer neue Trassenantraege. Diese Berechnung erfolgt durch den `TtrPhaseResolver` basierend auf dem Startdatum des Fahrplanjahres und dem aktuellen Datum.

### Phasen-Badge im TreeGrid

Neben jedem Fahrplanjahr im TreeGrid wird ein farbcodierter Badge angezeigt:

| Badge-Farbe | Phase | ProcessType | Bestellphase |
|---|---|---|---|
| **Gruen** | Annual Ordering (X-11 bis X-8.5) | ANNUAL_NEW (0) | Bestellphase 2 |
| **Gelb** | Late Ordering (X-8.5 bis X-2) | ANNUAL_LATE (1) | Bestellphase 3 |
| **Orange** | Ad Hoc (X-2 bis X) | AD_HOC (2) | Ad-hoc |
| **Grau** | Past oder Capacity Strategy/Model/Supply | — | — |

### Bestellphase 2 vs. Bestellphase 3

**Bestellphase 2 — Annual Ordering (regulaer):**

In dieser Phase steht der vollstaendige TTT-Prozess zur Verfuegung:
1. SEND_REQUEST → IM_RECEIPT → IM_DRAFT_OFFER → IM_FINAL_OFFER → ACCEPT_OFFER
2. Der IM kann Vorangebote machen, die der RA pruefen und ggf. mit Revision ablehnen kann
3. ProcessType = `ANNUAL_NEW` (Code 0)

**Bestellphase 3 — Late Ordering (verkuerzt):**

In dieser Phase entfaellt das Vorangebot:
1. SEND_REQUEST → IM_RECEIPT → IM_FINAL_OFFER → ACCEPT_OFFER
2. `IM_DRAFT_OFFER` ist **nicht** als Aktion verfuegbar
3. Der IM kann aus `RECEIPT_CONFIRMED` direkt ein `IM_FINAL_OFFER` machen
4. ProcessType = `ANNUAL_LATE` (Code 1)
5. Im UI zeigt die Prozess-Simulation eine Warnung bei Zuegen im NEW-Zustand

### Phasen-Info bei Prozess-Simulation

Wenn ein Zug sich im Zustand `NEW` befindet, zeigt das `ProcessSimulationPanel` eine Info-Box mit:
- Dem automatisch ermittelten ProcessType (z.B. "ProcessType=1 (ANNUAL_LATE)")
- Einer Warnung in Bestellphase 3: "Kein Vorangebot moeglich — direktes Endangebot"
- Dem Fahrplanjahr und der aktiven Phase

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

## Swagger UI

The Path Manager REST API is fully documented via interactive Swagger UI.

### Zugriff

Oeffnen Sie im Browser:

```
http://localhost:8085/swagger-ui/index.html
```

### Navigieren in Swagger UI

Die Endpoints sind nach Tags gruppiert:

| Tag | Endpoints | Beschreibung |
|---|---|---|
| **Path Manager** | 7 | CRUD-Operationen fuer Referenzzuege, Versionen und Betriebspunkte |
| **TTT Process Simulation** | 3 | State-Machine-Operationen (Transition, History, Available Actions) |
| **Timetable Diff** | 1 | Versionsvergleich zwischen Order-Seite und PM-Version |

### Authentifizierung

Die API-Endpoints sind aktuell ohne separate Authentifizierung zugaenglich (stateless SecurityFilterChain fuer `/api/**`). In einer Produktionsumgebung sollte ein API-Token oder OAuth2-Bearer-Token konfiguriert werden.

### Beispielaufrufe direkt in Swagger

1. Klicken Sie auf einen Endpoint (z.B. `GET /api/v1/pathmanager/trains`)
2. Klicken Sie auf **"Try it out"**
3. Fuellen Sie die Parameter aus (falls noetig)
4. Klicken Sie auf **"Execute"**
5. Die Antwort wird unterhalb angezeigt mit Status-Code, Headers und Body

## Haeufige Fehler

### "LazyInitializationException" bei API-Aufrufen

**Ursache**: Ein Entity-Feld wird ausserhalb einer aktiven Hibernate-Session geladen. Dies tritt auf, wenn lazy-geladene Collections in der Serialisierung aufgeloest werden.

**Loesung**: Die Controller-Schicht ist mit `@Transactional(readOnly = true)` annotiert und die Repositories verwenden `@EntityGraph` fuer Eager-Loading kritischer Collections (z.B. JourneyLocations). Falls der Fehler erneut auftritt, pruefen Sie, ob ein neues Feld im DTO-Mapper auf eine nicht eager-geladene Collection zugreift.

### "Invalid transition" bei Prozessschritt

**Ursache**: Die gewuenschte Aktion ist im aktuellen Zustand des Referenzzugs nicht erlaubt. Die TTT State Machine hat definierte Transitionen.

**Loesung**: Pruefen Sie den aktuellen Zustand des Zugs und die erlaubten Aktionen:
```
GET /api/v1/pathmanager/process/{referenceTrainId}/available-actions
```
Nur die zurueckgegebenen Aktionen koennen ausgefuehrt werden.

### Referenzzug wird nicht im TreeGrid angezeigt

**Ursache**: Der Zug wurde moeglicherweise einem Fahrplanjahr zugeordnet, das nicht im aktuellen Anzeigebereich liegt, oder der Zug-Datensatz wurde nicht korrekt persistiert.

**Loesung**:
1. Pruefen Sie ueber die API, ob der Zug existiert: `GET /api/v1/pathmanager/trains`
2. Pruefen Sie, ob ein Fahrplanjahr fuer den Gueltigkeitszeitraum existiert
3. Laden Sie die Path Manager-Ansicht neu (F5)

### "Position has already been sent" beim Senden

**Ursache**: Die FAHRPLAN-Position wurde bereits an den Fahrplanmanager gesendet. Das Feld `pmReferenceTrainId` auf der Position ist bereits belegt.

**Loesung**: Klicken Sie auf das teal-farbene Zug-Icon, um den bereits gesendeten Referenzzug im Fahrplanmanager anzuzeigen. Ein erneutes Senden derselben Position ist nicht moeglich.

### Diff zeigt keine Unterschiede

**Ursache**: Die Order-seitige Fahrplandaten und die aktuelle PM-Version sind identisch. Dies ist normal, wenn der Fahrplan nach dem Senden nicht vom IM modifiziert wurde.

**Loesung**: Fuehren Sie einen IM-Prozessschritt aus (z.B. IM_DRAFT_OFFER), der eine neue Version mit geaenderten Zeiten erstellt. Danach zeigt der Diff die Unterschiede.
