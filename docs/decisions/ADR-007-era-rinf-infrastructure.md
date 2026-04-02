# ADR-007: ERA RINF als Infrastruktur-Stammdatenquelle

## Status
Accepted

## Context
Der Fahrplan-Builder benoetigt Infrastrukturdaten fuer die Routenberechnung:
- **Operational Points (OPs)**: Betriebspunkte mit UOPID, Name, Koordinaten und Land
- **Sections of Line (SoLs)**: Streckenabschnitte zwischen zwei OPs mit Laengenangabe in Metern

Diese Daten muessen aus einer zuverlaessigen, aktuellen Quelle stammen und muessen laenderuebergreifend (mindestens CH und DE) verfuegbar sein.

Alternativen:
1. **Manuelle Pflege**: Betriebspunkte und Strecken manuell in der Datenbank anlegen — fehleranfaellig, nicht skalierbar
2. **OpenStreetMap**: Freie Geodaten — enthalten keine eisenbahnspezifischen Identifikatoren (UOPID), unstrukturiert
3. **ERA RINF (Register of Infrastructure)**: Offizielles europaeisches Infrastrukturregister mit standardisierten Daten und SPARQL-Endpoint

## Decision
Wir verwenden **ERA RINF** als primaere Quelle fuer Infrastruktur-Stammdaten. Die Daten werden als CSV-Dateien aus dem ERA SPARQL-Endpoint (`https://era.linkeddata.es/sparql`) exportiert und ueber die Einstellungsseite importiert.

Wichtige Implementierungsdetails:
- Der Import ist **atomar** (Replace-Import): bestehende Daten des gleichen Typs und Landes werden ersetzt
- Deutsche Betriebspunkte werden nach **UOPID dedupliziert** (die CSV enthaelt Duplikate)
- Vier **synthetische Grenzverbinder** (0m-Laenge) verknuepfen CH- und DE-Netz an Basel Bad Bf, Schaffhausen, Konstanz und Kreuzlingen
- Import-Logs protokollieren jeden Importvorgang

## Consequences
- **Positiv**: Standardisierte, europaweite Datenquelle mit offiziellen Identifikatoren
- **Positiv**: Kein manueller Pflegeaufwand fuer Stammdaten — Update durch erneuten Import
- **Positiv**: UOPID ermoeglicht spaetere Integration mit TTT-Systemen
- **Negativ**: RINF-Daten sind nicht immer vollstaendig — fehlende Streckenabschnitte fuehren zu "No route found"
- **Negativ**: Die synthetischen Grenzverbinder muessen manuell gepflegt werden, wenn neue Grenzuebergaenge hinzukommen
- **Negativ**: Aktuell nur CH und DE unterstuetzt; weitere Laender erfordern angepasste CSV-Exporte
