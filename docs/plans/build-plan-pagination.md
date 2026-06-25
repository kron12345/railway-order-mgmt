# Bauplan: Pagination / Lazy-Loading bei großen Datenmengen

> Geplant 2026-06-25 (frontend-design). Beratung: **Codex** (tiefe Repo-Analyse, deckt sich mit
> eigener Sicht) + eigene Synthese. **Gemini-CLI nicht verfügbar** (Tier-Fehler, Free-Tier
> abgekündigt). User-Entscheide: exakte Counts nur wo billig (sonst „geladen/mehr") · Auto-Scroll +
> fokussierbarer „mehr"-Sentinel · Start mit OP-Combos + Topology (Worst Offender).

## Leitidee

**Serverseitiges `Slice`-Loading als Datenmodell — bewusst nicht überall dieselbe UI-Metapher.**
Klassische Seitenzahlen (1 2 3) passen nicht zur Bloomberg-/Keyboard-Identität; nur für Sonderfälle
(Audit/Export). Je Flächentyp die passende Mechanik (s. u.).

### Signatur (Design)
Eine **persistente „Data-Readout"-Leiste** unten an jeder großen Liste — wie eine Terminal-Statuszeile
(JetBrains Mono, `--rom-text-muted`): zeigt geladenen Bereich + Filterstand + Keyboard-Hint und tickt
beim Scrollen hoch. Das ist die einzige „Bewegung" (Text-Update + dünne `aria-busy`-Hairline beim
Fetch; respektiert `prefers-reduced-motion`). Neue Komponente `DataReadout` (wiederverwendbar).

```
├───────────────────────────────────────────┤
│ ▌ 1–50 / ~2.3k · gefiltert 23 · ↓ mehr  ⌃K │
└───────────────────────────────────────────┘
```

## Architektur-Verträge (Backend)

- **`Slice<T>`** (fetch `pageSize+1`), **nicht** `Page<T>` — kein teurer Total-Count bei
  gefilterten/Join-Listen. Exakter `count(*)` nur wo billig (Topology).
- **List-DTOs/Projektionen** statt Entity+Collections. Badge-/Count-Werte (Positionen, Links,
  Status-Pills) über **Aggregat-Queries**, nicht über initialisierte Collections.
- **Kein `JOIN FETCH` auf Collections mit `Pageable`** (→ In-Memory-Pagination/Duplikate).
- **Stabile Sortierung mit ID-Tie-Breaker**, z. B. `valid_to asc nulls last, order_number asc, id asc`.
- **Postgres:** Indizes auf Sort-/Filterfelder; OP-Namens-/UOPID-Suche per Präfix (sonst `pg_trgm`/GIN).
- Service-Verträge (Beispiele): `Slice<OrderListItem> searchOrders(OrderListQuery, Pageable)`,
  `Slice<OperationalPointOption> searchOperationalPoints(String, Pageable)`,
  `Slice<DeadlineRow> findDeadlines(Status, Pageable)`, optional `long countX(filter)`.

## Keyboard / A11y (Bloomberg-treu)

`listbox/option`, `aria-selected`, Fokus bleibt in der Liste. Pfeile navigieren Geladenes; am Ende
lädt `↓`/`PageDown` die nächste Slice. „Weitere 50 laden"-Sentinel per Tab **und** `⌃K`-Command-Palette
erreichbar. `AriaLive`: „50 geladen, 150 sichtbar" / „Filter geändert, 23 Treffer". `aria-busy` beim
Fetch.

## Slices (in dieser Reihenfolge)

### P1 · OP-ComboBoxen lazy (Worst Offender)
- `OperationalPointRepository.search(filter, Pageable)` (Präfix; ggf. `pg_trgm`-Index) + `countSearch`.
- Wiederverwendbare `OperationalPointComboBox` (lazy `setItems(FetchCallback, CountCallback)`, ab 2
  Zeichen, Label `UOPID · Name · Land`, **ausgewählter Wert per ID nachladbar**).
- Ersetzt `opRepo.findAll()` in `ServicePositionDialog`, `TimetableRouteStep`, `AddStopForm`,
  `TimetableBuilderView`.
- **Verify:** Dialog öffnet ohne 19k-Load; Suche „Luz" liefert Treffer; Auswahl bleibt nach Reload.

### P2 · Topology-Grid lazy + Readout
- `TopologyTab`-Grid auf Lazy-DataProvider (`setItems(FetchCallback, CountCallback)` / `setItemsPageable`),
  Server-Filter + Server-Sortierung (Spalten auf Backend-Felder gemappt). Kein `findAll()`,
  kein `setAllRowsVisible(true)`.
- Neue `DataReadout`-Komponente, hier mit **exaktem** `count(*)`.
- **Verify:** Grid scrollt virtuell durch 19k, Suche/Sortierung serverseitig, Readout „1–100 / 19.341".

### P3 · Slice-DTOs + Such-Service für Orders/Businesses
- Projektions-DTOs `OrderListItem`/`BusinessListItem` + `searchOrders(OrderListQuery, Pageable): Slice`
  + Aggregat-Queries für Karten-Badges. Stabile Sortierung + Tie-Breaker. (Noch ohne UI-Umbau.)
- **Verify:** Service-Query liefert korrekte Slices/Counts (DB-Gegencheck).

### P4 · MasterDetailLayout lazy + Readout-Signatur
- `MasterDetailLayout<T>` um einen Lazy-`Slice`-Contract erweitern (statt `setItems(List)`): Karten +
  Shortcuts bleiben, Filter/Type-ahead werden **Server-Query-Objekte** statt `Predicate` über Vollliste.
- Auto-Nachladen nahe Listenende + fokussierbarer „weitere 50 laden"-Sentinel + `DataReadout`
  (Orders/Businesses: „geladen/mehr", kein Total) + `AriaLive`.
- Anwenden auf `/orders` + `/businesses`.
- **Verify:** Liste lädt initial 50, scrollt nach, Filter serverseitig, Shortcuts (g o/g b, ↑/↓) intakt,
  0 pageerrors.

### P5 · Fristen/Offene als Sectioned-Lazy-List
- `FristService`/`OffenePositionen`: Gruppierung + Slicing aus SQL/Service (nicht im UI kappen). Pro
  Gruppe erste 25 + Gruppenzähler + „weitere laden". Ersetzt das aktuelle `MAX_PER_GROUP`-Kappen.
- **Verify:** je Gruppe Nachladen, Zähler stimmen.

### P6 · PathManager-Provider auf echte DB-Pagination
- `findAll().stream().skip().limit()` → echte Repository-`Pageable`-Queries im
  `AbstractBackEndHierarchicalDataProvider`.
- **Verify:** TreeGrid lädt seitenweise aus der DB.

### P7 · Politur
- Optionale Counts wo sinnvoll, Query-Param-State (Filter in der URL), `⌃K`-Integration der
  „mehr laden"-Aktion, ARIA-Feinschliff, Index-Migrationen, evtl. `CatalogTab` lazy.

## Vaadin-24.7-Fallstricke (aus Codex verifiziert)
- `Grid.setPageSize()` = Fetch-/Cache-Größe, **keine** UI-Seite.
- `setAllRowsVisible(true)` hebelt Virtualisierung aus.
- Grid-Sortierung muss auf Backend-Felder gemappt werden; Component-Columns brauchen Sort-Properties.
- ComboBox-Wert kann außerhalb der aktuellen Filterseite liegen → ausgewählte OPs **per ID** nachladbar.
- `Grid`/`ComboBoxBase` haben in 24.7.4 `setItemsPageable(...)` (Fetch-only + Fetch+Count-Overload).

## Abschluss
Je Slice: Compile/Spotless/ArchUnit + Commit + Codex + E2E-Screenshot. Am Ende E2E-Durchlauf,
Demo-Daten-Reset, Push. Danach: Customer-/Geschäfts-Views.
