# ADR-004: Accordion + Status-Chips als Primaersicht fuer Auftraege

## Status
Accepted

## Context
Die urspruengliche Auftragsliste verwendete ein flaches Grid mit einer Zeile pro Auftrag. Mit wachsender Anzahl von Positionen und Metadaten wurde die Darstellung unuebersichtlich. Disponenten benoetigen einen schnellen Ueberblick ueber Auftraege und deren Positionen, ohne jede Detailseite einzeln oeffnen zu muessen.

Alternativen:
1. **Grid mit Expand-Row**: Technisch moeglich, aber bei Vaadin Grid visuell starr und schwer mit Kacheln kombinierbar
2. **Card-Layout**: Flexibel, aber bei vielen Auftraegen entsteht zu viel vertikaler Scrollbedarf
3. **Accordion mit Positions-Kacheln**: Kompakte geschlossene Ansicht, Detail-on-Demand beim Aufklappen

## Decision
Die Auftragsliste verwendet ein **Accordion-Layout** mit folgenden Elementen:
- **Geschlossene Zeile**: Auftragsnummer, Name, Status-Chip (farbkodiert), Gueltigkeitszeitraum, Positionsanzahl, Kommentar-Vorschau
- **Aufgeklappter Bereich**: Positionen als Kacheln (PositionTile) mit Route, Zeitfenster, Tags, Bestellanzahl
- **Summary-Metriken**: Oberhalb der Liste Gesamtzaehler und Aufschluesselung nach Status
- **Status-Chips**: Farbkodierte Badges mit Zaehlern fuer eine schnelle visuelle Einordnung

## Consequences
- **Positiv**: Kompakte Uebersicht — Disponenten sehen auf einen Blick den Status aller Auftraege und koennen gezielt aufklappen
- **Positiv**: Kacheln innerhalb des Accordion ermoeglichen eine reichhaltigere Darstellung als Grid-Zeilen (Route, Tags, Zeitfenster)
- **Negativ**: Kein sortier- oder filterbares Grid — Sortierung und Filter muessen separat implementiert werden
- **Negativ**: Bei vielen Positionen pro Auftrag wird der aufgeklappte Bereich lang; Pagination oder Virtualisierung waere bei > 50 Positionen noetig
- Die Komponente `OrderAccordionRow` wurde als eigene Klasse extrahiert, um die Dateigroesse der `OrderListView` unter 500 Zeilen zu halten
