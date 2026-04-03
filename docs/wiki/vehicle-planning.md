# Fahrzeugplanung (Vehicle Planning) -- User Guide

## Overview

Die Fahrzeugplanung (Vehicle Planning / Umlaufplanung) ermoeglicht die visuelle Zuordnung von Referenzzuegen aus dem Fahrplanmanager zu physischen Fahrzeugen. Sie unterstuetzt:

- Erstellen und Verwalten von Umlaufplaenen pro Fahrplanjahr
- Hinzufuegen von Fahrzeugen (Triebzuege, Lokomotiven, Wagengarnituren)
- Drag-&-Drop-Zuordnung von Zuegen auf Fahrzeuge in einem Gantt-Diagramm
- Automatische Konflikterkennung (Zeitueberlappung, Standort-Mismatch)
- Wochentagsbezogene Planung (Montag bis Sonntag)

Die Fahrzeugplanung ist ueber die Seitenleiste unter "Fahrzeugplanung" erreichbar.

## Navigation

### Seitenleiste

Klicken Sie in der Hauptnavigation auf **Fahrzeugplanung**. Die Route ist `/vehicleplanning`.

### Bildschirmaufbau

Die Ansicht ist als SplitLayout 20/80 organisiert:

```
+-------------------+-----------------------------------------------+
| Zugpalette (20%)  | Gantt-Diagramm (80%)                          |
|                   |                                               |
| [Suchfeld]        | Zeitlineal 00:00 --- 06:00 --- 12:00 --- 18:00 --- 24:00 |
|                   |                                               |
| IC 123            | Fahrzeug 1: [====IC 123====] [====IC 456====] |
| IC 456            | Fahrzeug 2: [======RE 789=======]             |
| RE 789            | Fahrzeug 3: [==EC 321==]    [====IC 654====]  |
| EC 321            |                                               |
| IC 654            +-----------------------------------------------+
|                   | Konfliktpanel                                  |
|                   | (!) Zeitueberlappung: Fahrzeug 3, Mittwoch    |
+-------------------+-----------------------------------------------+
```

Oberhalb des Gantt-Diagramms befinden sich:
- **Fahrplanjahr-Dropdown**: Waehlt das Fahrplanjahr aus (z.B. 2026, 2027)
- **Umlaufplan-Dropdown**: Waehlt den Umlaufplan innerhalb des Fahrplanjahrs
- **"+ Neuer Umlaufplan"**-Button: Erstellt einen neuen Umlaufplan
- **Wochentag-Auswahl**: Montag bis Sonntag (bestimmt, welcher Tag im Gantt angezeigt wird)

## Umlaufplan erstellen

1. Waehlen Sie ein **Fahrplanjahr** aus dem Dropdown (z.B. "Fahrplanjahr 2027")
2. Klicken Sie auf **"+ Neuer Umlaufplan"**
3. Im Dialog:
   - **Name**: Pflichtfeld (z.B. "FLIRT S-Bahn Olten-Aarau")
   - **Beschreibung**: Optionales Freitextfeld
4. Klicken Sie auf **Erstellen**
5. Der neue Umlaufplan wird automatisch ausgewaehlt

## Fahrzeuge hinzufuegen

Innerhalb eines Umlaufplans koennen Sie Fahrzeuge anlegen:

1. Klicken Sie auf **"+ Fahrzeug"** (oder den entsprechenden Button)
2. Fuellen Sie die Felder aus:

| Feld | Pflicht | Beschreibung | Beispiel |
|---|---|---|---|
| **Bezeichnung** | Ja | Name des Fahrzeugs | "FLIRT RABe 526 201" |
| **Fahrzeugtyp** | Ja | Typ des Fahrzeugs (Dropdown) | MULTIPLE_UNIT |
| **Fahrzeugklasse** | Nein | Optionale Klassifizierung | "RABe 526" |

### Fahrzeugtypen

| Typ | Beschreibung | Typische Beispiele |
|---|---|---|
| **MULTIPLE_UNIT** (Triebzug) | Selbstfahrender Triebzug | FLIRT (RABe 526), Giruno, ICE, Stadler KISS |
| **LOCOMOTIVE** (Lokomotive) | Triebfahrzeug ohne Personenwagen | Re 460, Re 620, Vectron, Taurus |
| **COACH_SET** (Wagengarnitur) | Passiv gezogene Wagenkomposition | EW IV, IC2000, Dosto |

## Drag & Drop — Zuege auf Fahrzeuge zuordnen

### Zuege aus der Palette ziehen

1. Waehlen Sie den gewuenschten **Wochentag** (z.B. Montag)
2. In der **Zugpalette** (links) sehen Sie alle Referenzzuege des Fahrplanjahrs
3. Nutzen Sie das **Suchfeld**, um nach OTN (z.B. "IC 123") oder Route zu filtern
4. **Ziehen** Sie einen Zug (Drag) aus der Palette auf die gewuenschte **Fahrzeugzeile** im Gantt-Diagramm (Drop)
5. Der Zug erscheint als farbiger Block auf der Zeitleiste

### Zugblock-Darstellung

Jeder zugeordnete Zug wird als rechteckiger Block angezeigt:
- **Position**: Horizontal entsprechend Abfahrts- und Ankunftszeit des Referenzzugs
- **Breite**: Proportional zur Gesamtfahrzeit
- **Beschriftung**: OTN und Route (z.B. "IC 123 Zuerich-Basel")
- **Farbe**: Abhaengig vom Fahrzeugtyp

### Kupplungsposition (CouplingPosition)

Bei der Zuordnung kann die Kupplungsposition festgelegt werden:

| Position | Beschreibung | Anwendungsfall |
|---|---|---|
| **FULL** | Ganzer Zug (Standard) | Einfacher Umlauf, ein Fahrzeug fuehrt den gesamten Zug |
| **FRONT** | Vorderer Zugteil | Fluegel-/Mehrfachtraktionsbetrieb, Fahrzeug fuehrt den vorderen Teil |
| **REAR** | Hinterer Zugteil | Fluegel-/Mehrfachtraktionsbetrieb, Fahrzeug fuehrt den hinteren Teil |

## Konflikterkennung

Der `ConflictDetectionService` prueft bei jeder Aenderung automatisch auf Planungskonflikte. Die Ergebnisse erscheinen im **Konfliktpanel** unterhalb des Gantt-Diagramms.

### Erkannte Konflikttypen

| Severity | Typ | Beschreibung |
|---|---|---|
| **ERROR** (Rot) | Zeitueberlappung | Zwei Zuege sind zur gleichen Zeit dem gleichen Fahrzeug zugewiesen. Die Zeitbereiche (Abfahrt erstes OP bis Ankunft letztes OP) ueberlappen sich. |
| **WARNING** (Gelb) | Standort-Mismatch | Der Ankunftsort (letztes OP) des vorherigen Zuges stimmt nicht mit dem Abfahrtsort (erstes OP) des naechsten Zuges ueberein. Das Fahrzeug muesste sich ohne geplante Fahrt bewegen. |

### Wendezeit (Turnaround Time)

Zwischen zwei aufeinanderfolgenden Zuegen eines Fahrzeugs sollte eine angemessene Wendezeit eingeplant werden. Der Conflict Detection Service erkennt, wenn die Uebergangszeit zu knapp ist oder die Standorte nicht uebereinstimmen.

## Fahrzeugoperationen (TTT Vehicle Operations)

Fuer detaillierte Betriebsplanung koennen pro Umlaufeintrag Operationen erfasst werden:

| Feld | Beschreibung |
|---|---|
| **Ortsname** | Betriebspunkt der Operation (z.B. "Olten") |
| **Aktivitaetscode** | TTT-Code fuer die Operation |
| **Assoziierter Zug (OTN)** | Bei Kupplungs-/Fluegelungsvorgaengen: OTN des anderen Zuges |
| **Kompositionsabschnitt** | Abschnitt in der Zugkomposition |
| **Kommentar** | Freitext |

### Relevante TTT-Aktivitaetscodes fuer Fahrzeugoperationen

| Code | Beschreibung |
|---|---|
| 0010 | Kuppeln von Zugteilen |
| 0011 | Entkuppeln / Trennen von Zugteilen |
| 0012 | Triebfahrzeugwechsel |
| 0017 | Abkuppeln |
| 0044 | Wenden ohne Triebfahrzeugwechsel (Pendelzug) |
| 0045 | Wenden mit Triebfahrzeugwechsel |

## Technische Hinweise

- VP-Daten werden **nicht** von Hibernate Envers auditiert, da es sich um Planungsdaten handelt
- Die Konflikterkennung liest Zug-Zeitdaten direkt aus den `PmJourneyLocations` der Referenzzuege
- Es gibt keine eigene REST API fuer Vehicle Planning — alle Operationen laufen ueber den Vaadin Flow Server-Side UI Layer
- Umlaufplaene sind an ein Fahrplanjahr gebunden (FK auf `pm_timetable_years`)
- Beim Loeschen eines Umlaufplans werden alle zugehoerigen Fahrzeuge, Eintraege und Operationen kaskadierend geloescht

## Haeufige Fehler

### Keine Zuege in der Palette sichtbar

**Ursache**: Es existieren keine Referenzzuege im gewaehlten Fahrplanjahr. Zuege muessen zuerst ueber den Fahrplanmanager erstellt werden (z.B. durch "An PM senden" aus einer FAHRPLAN-Position).

**Loesung**: Erstellen Sie Fahrplanpositionen, senden Sie sie an den Fahrplanmanager, und waehlen Sie dann das korrekte Fahrplanjahr in der Fahrzeugplanung.

### Konflikte werden nicht angezeigt

**Ursache**: Konflikte werden nur fuer den aktuell sichtbaren Wochentag berechnet.

**Loesung**: Wechseln Sie durch die einzelnen Wochentage (Mo-So), um Konflikte fuer jeden Tag zu pruefen.

### Drag & Drop funktioniert nicht

**Ursache**: Moegliche Browser-Kompatibilitaetsprobleme oder fehlende Berechtigungen.

**Loesung**:
1. Stellen Sie sicher, dass Sie die Rolle DISPATCHER oder ADMIN haben (VIEWER kann keine Zuordnungen vornehmen)
2. Verwenden Sie einen aktuellen Browser (Chrome, Firefox, Edge)
3. Laden Sie die Seite neu (F5)
