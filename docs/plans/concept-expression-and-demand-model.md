# Konzept: Ausprägungen & Bedarfe vertieft (Verkehrstage auf zwei Ebenen)

> Status: **Konzept zur Freigabe** (2026-06-25). User-Entscheide: Verkehrstage auf **Ausprägung +
> Bedarf** · Überlappung = **Umverteilen + Modification (mit Bestätigung)** · Optik = **volle-Breite-
> Karten ohne Einrückung** · **Konzept zuerst, Bau vor der Pagination**. Baut auf K1 (Zug/Ausprägung/
> Version) + P1 (`OperationalPointComboBox`) auf.

## Kernidee: zwei Ebenen von Verkehrstagen

| Ebene | Verkehrstage | Regel | Wofür |
|---|---|---|---|
| **Zug** (Auftragsposition) | — | stabile Identität | findbar über (alte) OTN/TRID |
| **Ausprägung** | eigene, **disjunkt** unter Geschwistern | andere Trasse/Zeit/Fahrzeug | Mo–Fr 2×Flirt vs. Sa–So 1×Flirt |
| **Bedarf** (ResourceNeed) | eigene, **⊆ Ausprägungs-Tage**, dürfen sich überlappen | „wann gilt dieser Bedarf" + von/nach | Wochenende 2 Zugbegleiter, gleiche Trasse |

So muss eine reine Bedarfs-Variation (gleiche Trasse, anderes Personal) **keine** neue Ausprägung
erzeugen — sie lebt auf dem Bedarf. Eine Ausprägung trifft mehrere Bedarfe; ein Bedarf kann nur an
bestimmten Tagen gelten.

**Datum-genau, nicht nur Wochentag:** Damit das Umverteilen einzelner Tage funktioniert, sind
Verkehrstage ein **Datums-Set** (kein reines Mo–Fr-Muster). Wir nutzen das vorhandene
`OrderPosition.validity` (JSONB-Datumsliste, `ValidityJsonCodec`) + `ValidityCalendar` als Picker; das
`weekdays`-Feld (K1-S4) wird zur **Schnellauswahl/Label** („Mo–Fr"), die maßgebliche Belegung ist die
Datumsliste.

## Datenmodell-Änderungen

- **Ausprägung:** vorhanden (`variantOf`-Hierarchie, `validity`, `weekdays`). Keine neuen Felder; die
  Disjunktheit wird jetzt datums-genau + interaktiv (s. u.).
- **Bedarf (`ResourceNeed`):** neu
  - `validity` (JSONB-Datums-Set; ⊆ Tage der Ausprägung) — eigene Verkehrstage des Bedarfs.
  - `from_location` / `to_location` (VARCHAR) — von/nach, nur relevant bei FAHRPLAN-Position.
  - (vorhanden bleiben `valid_from`/`valid_to` als grober Rahmen.)
  - Migration `Vxx` + `_audit`.

## Anlege-Fluss einer Ausprägung (typgleich, vorbefüllt)

- „+ Ausprägung" öffnet den **typrichtigen Editor**, **vorbefüllt vom Original** (Eltern-Zug):
  - **FAHRPLAN** → Fahrplan-Builder (`TimetableBuilderView`) im **Kind-Modus** (`variantOf`=Eltern); der
    Builder kann heute schon bestehende Positionen laden (`positionId`-Param) → um „Kind anlegen"
    erweitern. Du speicherst nur die Änderungen.
  - **LEISTUNG** → `ServicePositionDialog`, vorbefüllt.
- Der generische `ExpressionDialog` entfällt; die **Verkehrstage-Auswahl** wird ein Schritt/Panel im
  Editor (s. u.).
- Erzeugt ein Kind gleichen Typs (`variantType = AUSPRAEGUNG`).

## Verkehrstage-Picker mit Belegung + Umverteilung

- Kalender (Erweiterung `ValidityCalendar`), begrenzt auf die Tage der Auftragsposition.
- **Belegte Tage** (von Geschwister-Ausprägungen) sind **markiert**; Tooltip zeigt die belegende
  Ausprägung.
- Wähle ich einen belegten Tag → **Bestätigungsdialog**: „Tag X gehört zu ‚Sa–So' — übernehmen?" Bei
  Ja:
  1. Tag wandert in die neue/aktuelle Ausprägung.
  2. Die andere Ausprägung wird **gekürzt** (Tag aus ihrer `validity` entfernt).
  3. Auf der gekürzten Ausprägung wird automatisch eine **`MODIFICATION`-Version** erfasst
     (`OrderPositionVersion`, Quelle MODIFICATION, Summary „Verkehrstag X abgegeben an …").
- Damit ist die Disjunktheit interaktiv statt hartem Reject (`ExpressionConflictException` entfällt für
  diesen Pfad).

## Bedarfe mit eigenen Verkehrstagen + von/nach

- `ResourceDialog` bekommt:
  - einen **Kalender** (`ValidityCalendar`, begrenzt auf die Tage der **Ausprägung** des Bedarfs) für
    die Bedarfs-Verkehrstage. Default = alle Tage der Ausprägung.
  - bei FAHRPLAN-Position: **von/nach** über zwei **`OperationalPointComboBox`** (lazy, aus P1).
- Bedarfe innerhalb einer Ausprägung dürfen sich Tage **teilen** (kein Disjunktheits-Check) — man
  braucht ja Lokführer **und** Zugbegleiter am selben Tag.
- Anzeige je Bedarf: Mengen + Verkehrstage-Kurzlabel + Route (bei FAHRPLAN).

## Optik: volle-Breite-Karten (ohne Einrückung)

```
S2 18000 · OTN 18000 · war 17000                         [+ Ausprägung]
┌ Mo–Fr · 2×Flirt · Zürich HB → Zug · 08:12 ───────────────────────┐
│ Bedarfe:  1× Lokführer (Mo–Fr)   ·   2× Flirt (Mo–Fr)            │
│ Bestellungen: …      Änderungen: …      [Bearbeiten] [Kalender]   │
└──────────────────────────────────────────────────────────────────┘
┌ Sa–So · 1×Flirt · Zürich HB → Zug · 08:30 ───────────────────────┐
│ Bedarfe:  1× Lokführer (Sa–So)   ·   2× Zugbegleiter (Sa) · 1× (So)│
│ …                                                                │
└──────────────────────────────────────────────────────────────────┘
```

- Jede Ausprägung = eigenständige Karte mit Kopf (Verkehrstage · OTN · Route · Zeit) + eigenem
  Bedarfe-Panel + Bestellungen + Änderungs-Feed. Keine Einrückung; der Zug-Header bündelt sie.
- Bestand (flache Position) = ein Zug mit genau einer Karte.

## Bestellpositionen (offene Frage geklärt)

Bestellpositionen hängen weiter am **Bedarf** (`PurchasePosition` → `ResourceNeed`). Da der Bedarf jetzt
eigene Verkehrstage trägt, ist die R²P-Variation (2 Begleiter am WE, 1 unter der Woche) sauber
abbildbar: zwei Personal-Bedarfe mit unterschiedlichen Verkehrstagen, je eigene Bestellposition — bei
**gleicher Trasse** (eine Ausprägung).

## Slice-Plan (Bau, vor der Pagination)

1. **A-S1 · Bedarf-Modell:** `ResourceNeed.validity` + `from_location`/`to_location` + Migration + Audit.
2. **A-S2 · Bedarf-UI:** `ResourceDialog` Kalender (begrenzt auf Ausprägungs-Tage) + von/nach (lazy
   OP-Combos); Anzeige je Bedarf mit Verkehrstage/Route.
3. **A-S3 · Karten-Optik:** Ausprägungen als volle-Breite-Karten (ohne Einrückung), je eigenes
   Bedarfe-Panel.
4. **A-S4 · Verkehrstage-Picker:** `ValidityCalendar` mit Belegungs-Markierung + Tooltip;
   Umverteilung + Modification-auf-Geschwister (mit Bestätigung); Disjunktheit datums-genau.
5. **A-S5 · Typgleicher Anlege-Editor:** „+ Ausprägung" → Fahrplan-Builder (Kind-Modus, vorbefüllt) /
   ServicePositionDialog (vorbefüllt); `ExpressionDialog` ablösen.
6. **Abschluss:** E2E über alle Flächen, Seed eines Mehr-Bedarf-Beispiels (WE-Begleiter), Reset, Push.

Je Slice: Compile/Spotless/ArchUnit + Commit + Codex + E2E. Danach zurück zur Pagination (P1b/P2…).

## Offene Detailfragen (für die Bau-Phase)

- Verkehrstage-Default beim Anlegen: leeres Set vs. „alle noch freien Tage" vorbelegt?
- Kürzt das Umverteilen auch, wenn dadurch eine Geschwister-Ausprägung **0 Tage** hätte → löschen oder
  blocken? **(A-S4 Ist-Stand: erlaubt — die Geschwister-Ausprägung bleibt mit `validity=null` / 0 Tagen
  bestehen; der Dialog verhindert nur das Speichern der *aktiven* Ausprägung mit 0 Tagen. Codex-P2,
  bewusst zurückgestellt bis Produkt-Entscheid löschen-vs-blocken-vs-warnen.)**
- **Versionsnummern-Race** (A-S4, Codex/Review-P3, zurückgestellt): `recordDayHandover` bildet die
  nächste Versionsnummer per `max+1` ohne Lock — wie das bestehende Muster (`PathProcessEngine`
  .createVersionFromLatest). Bei gleichzeitiger Umverteilung auf **dieselbe** Geschwister-Ausprägung
  kann die Unique-Constraint greifen; die ganze Operation ist eine atomare `@Transactional`-Einheit
  (sauberer Rollback, keine Korruption, seltener Mehrbenutzer-Transient). Fix später: DB-Sequenz oder
  pessimistischer Lock.
- Builder-Kind-Modus: komplette Trasse vorbefüllt editierbar, oder nur Zeiten/Fahrzeug (Route gesperrt)?
