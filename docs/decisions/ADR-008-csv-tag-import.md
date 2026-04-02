# ADR-008: CSV-Import fuer Schlagwort-Katalog statt SQL-Seed

## Status
Accepted

## Context
Die Anwendung verwendet vordefinierte Schlagwoerter (Tags) zur Klassifizierung von Auftraegen und Positionen. Der initiale Ansatz war, Tags als SQL-Seed-Daten in der Flyway-Migration zu definieren. Dies hat sich als unpraktisch erwiesen:
- Aenderungen am Tag-Katalog erfordern eine neue Migration
- Fachbereiche koennen den Katalog nicht selbst pflegen
- Verschiedene Umgebungen (Entwicklung, Test, Produktion) benoetigen unterschiedliche Kataloge

Alternativen:
1. **SQL-Seeds in Migrationen**: Einfach, aber statisch und nicht durch Fachbenutzer aenderbar
2. **Admin-GUI mit Einzelerfassung**: Flexibel, aber aufwaendig fuer groessere Kataloge
3. **CSV-Import ueber Admin-UI**: Massenimport per Datei-Upload, einfach zu verstehen

## Decision
Der Schlagwort-Katalog wird per **CSV-Import** ueber die Admin-Einstellungsseite gepflegt. Das CSV-Format:

```csv
name,category,sortOrder,active
Gueterverkehr,ORDER,10,true
Personenverkehr,ORDER,20,true
Rangierfahrt,POSITION,10,true
International,GENERAL,10,true
```

- **category**: ORDER (Auftraege), POSITION (Positionen), GENERAL (beide Kontexte)
- **sortOrder**: Reihenfolge in der Auswahlliste
- **active**: Deaktivierte Tags erscheinen nicht in der Auswahl, bleiben aber auf zugewiesenen Objekten

Bestehende SQL-Seeds wurden entfernt. Die CSV-Quelldatei liegt unter `data/seeds/predefined-tags.csv`.

## Consequences
- **Positiv**: Fachbenutzer (ADMIN) koennen den Katalog selbststaendig per CSV-Upload aktualisieren
- **Positiv**: Verschiedene Umgebungen koennen unterschiedliche CSV-Dateien verwenden
- **Positiv**: Deaktivierung ermoeglicht eine saubere Lebenszyklusverwaltung ohne Datenverlust
- **Negativ**: Keine Validierung auf CSV-Ebene — fehlerhafte Dateien fuehren zu Import-Fehlern
- **Negativ**: Kein Diff-Mechanismus — beim Import werden Tags mit gleichem Namen aktualisiert, aber verwaiste Tags nicht automatisch deaktiviert
