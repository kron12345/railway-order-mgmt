# ADR-005: Kompakter Order-Header + Dialoge/Fullscreen statt ueberladener Einzelseite

## Status
Accepted

## Context
Die Auftragsdetailseite muss verschiedene Positionstypen mit unterschiedlicher Komplexitaet abbilden:
- **LEISTUNG-Positionen**: Relativ wenige Felder (Name, Leistungsart, Von/Nach, Zeiten, Tags, Kommentar, Gueltigkeit)
- **FAHRPLAN-Positionen**: Hochkomplexe Bearbeitung mit Routing, Karte, mehrzeiliger Tabelle, Zeitpropagation

Alternativen:
1. **Alles auf einer Seite**: Alle Felder und Editoren inline in der Detailansicht — wird bei FAHRPLAN unuebersichtlich
2. **Einheitlicher Fullscreen-Editor**: Jede Position im Fullscreen — zu schwerfaellig fuer einfache LEISTUNG-Positionen
3. **Hybrid: Dialog fuer LEISTUNG, Fullscreen fuer FAHRPLAN**: Passt die UI-Komplexitaet an den Positionstyp an

## Decision
Wir verwenden einen **hybriden Ansatz**:
- **Order-Header**: Kompakte Darstellung mit Status-Badge, Metadaten und Edit-/Delete-Buttons. Kein Inline-Editing — das Formular oeffnet sich separat.
- **LEISTUNG-Positionen**: Erstellung und Bearbeitung ueber einen `ServicePositionDialog` (modaler Vaadin-Dialog mit Formularfeldern, OP-Auswahl, Gueltigkeitskalender)
- **FAHRPLAN-Positionen**: Erstellung und Bearbeitung im Fullscreen `TimetableBuilderView` (eigene Route, eigenes Layout, zwei Schritte)

## Consequences
- **Positiv**: LEISTUNG-Positionen lassen sich schnell im Dialog bearbeiten, ohne die Detailseite zu verlassen
- **Positiv**: Der Fahrplan-Builder hat den vollen Bildschirm fuer Karte, Tabelle und Editor-Panel
- **Positiv**: Die Detailseite bleibt uebersichtlich — sie zeigt nur den Header und die Positionszeilen
- **Negativ**: Zwei verschiedene Bearbeitungsmodelle (Dialog vs. Fullscreen) muessen verstanden werden
- **Negativ**: Der Dialog-Ansatz skaliert nicht fuer Positionstypen, die zukuenftig komplexer werden koennten
