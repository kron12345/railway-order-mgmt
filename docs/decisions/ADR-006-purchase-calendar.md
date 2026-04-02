# ADR-006: Bestellkalender mit TTR-Phasen fuer Kapazitaetsbestellung

## Status
Accepted

## Context
Fuer jede Auftragsposition muessen Kapazitaetsbestellungen (Purchase Positions) an den Infrastrukturbetreiber gesendet werden. Der europaeische Fahrplanbestellprozess folgt einem festgelegten Zeitschema mit TTR-Phasen (Timetable Redesign):
- **X-11**: Erste Bestellrunde ca. 11 Monate vor Fahrplanwechsel
- **X-8**: Zweite Bestellrunde
- **X-5**: Dritte Bestellrunde
- **Ad-hoc**: Kurzfristige Bestellungen

Bestellungen muessen nach Status (DRAFT, SUBMITTED, CONFIRMED, REJECTED) verfolgt und den jeweiligen Phasen zugeordnet werden koennen.

Alternativen:
1. **Einfache Tabelle**: Pro Position eine Liste der Bestellungen — funktional, aber ohne zeitlichen Kontext
2. **Kalenderansicht**: Bestellungen an den Gueltigkeitstagen der Position dargestellt — visuell ansprechend, aber komplex
3. **Panel mit Summary + Grid + Details**: Kombination aus Zusammenfassung, tabellarischer Liste und Detailansicht

## Decision
Wir implementieren einen **PurchaseCalendarPanel** als aufklappbares Panel unterhalb jeder Auftragsposition in der Detailansicht. Das Panel zeigt:
- **Summary**: Bestellanzahl nach Status als kompakte Badges
- **Grid**: Tabellarische Auflistung der Bestellungen mit Datum, Status, TTR-Phase und Aktionen
- **Details**: Bei Auswahl einer Bestellung erweiterte Informationen

Der Kalender ist pro Position separat abrufbar ueber einen Kalender-Button in der Positionszeile.

## Consequences
- **Positiv**: Disponenten sehen auf einen Blick, welche Bestellungen in welcher Phase stehen
- **Positiv**: Die TTR-Phasen sind explizit modelliert und koennen spaeter fuer Validierungen und Fristenwarnungen genutzt werden
- **Positiv**: Das Panel ist optionsweise ein-/ausklappbar und belastet die Detailseite nicht dauerhaft
- **Negativ**: Die Zuordnung der TTR-Phasen ist aktuell manuell — eine automatische Phasenberechnung anhand des Fahrplanjahres waere sinnvoll
- **Negativ**: Massenbestellungen (z.B. alle Positionen eines Auftrags gleichzeitig bestellen) sind noch nicht unterstuetzt
