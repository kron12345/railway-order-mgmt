# ADR-009: FAHRPLAN-Positionen verlinken auf timetable_archives ueber CAPACITY-Ressourcenbedarf

## Status
Accepted

## Context
FAHRPLAN-Positionen enthalten umfangreiche Fahrplandaten: die komplette Routentabelle mit Zeiten, Aktivitaeten, Zeitmodi, Propagationseinstellungen und Gueltigkeitsinformationen. Diese Daten muessen persistent gespeichert werden.

Zwei grundsaetzliche Ansaetze:
1. **Direkt auf der Position**: Alle Fahrplandaten als JSON-Spalte(n) in `order_positions` speichern
2. **Separates Archiv**: Fahrplandaten in einer eigenen Tabelle `timetable_archives` speichern und ueber eine Verknuepfung referenzieren

Das Argument fuer die Trennung: `order_positions` ist eine generische Tabelle fuer alle Positionstypen (LEISTUNG, FAHRPLAN etc.). Fahrplanspezifische Daten dort einzubetten wuerde die Tabelle mit Spalten belasten, die nur fuer einen Typ relevant sind. Ausserdem ermoeglicht eine separate Tabelle spaetere Versionierung und unabhaengige Audit-Trails.

## Decision
FAHRPLAN-Positionen speichern den Detailfahrplan **nicht selbst**, sondern verlinken 1:1 auf einen Eintrag in `timetable_archives` ueber einen **CAPACITY-Ressourcenbedarf** (`resource_needs`):

```
order_positions (FAHRPLAN)
  -> resource_needs (type=CAPACITY)
    -> timetable_archives (JSON mit der vollstaendigen Tabelle)
```

- `timetable_archives` enthaelt die komplette Routentabelle als JSON (`table_data`), Metadaten und den OTN
- `resource_needs` mit `type=CAPACITY` dient als Verknuepfung und traegt die Archiv-ID als Foreign Key
- Beim Speichern im Builder werden Kerndaten (Von/Nach, Start/Ende, OTN, Tags, Gueltigkeit) auf die `order_positions`-Zeile gespiegelt, damit die Auftragsansicht ohne Join auf das Archiv funktioniert

## Consequences
- **Positiv**: Saubere Trennung zwischen generischen Positionsdaten und fahrplanspezifischem Detail
- **Positiv**: Das Archiv kann unabhaengig versioniert und auditiert werden
- **Positiv**: Die Verlinkung ueber CAPACITY-Ressourcen ist erweiterbar fuer zukuenftige Ressourcentypen
- **Negativ**: Die 1:1-Beziehung ueber eine Zwischentabelle (`resource_needs`) ist indirekt und erfordert sorgfaeltige Abfragen
- **Negativ**: Beim Speichern muessen Metadaten auf zwei Tabellen synchron geschrieben werden (Archiv + Position)
- **Negativ**: Versionierung (mehrere Archive pro Position) ist noch nicht implementiert — aktuell wird das Archiv bei jedem Speichern ueberschrieben
