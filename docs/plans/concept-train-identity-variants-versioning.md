# Konzept: Zug-Identität, Ausprägungen (OTN × Verkehrstage) und Versionierung

> Status: **Konzept zur Freigabe** (2026-06-25). Bau erst **nach** Pagination + Customer-/Geschäfts-Views
> (User-Entscheid). Dieses Dokument ist die fachliche/technische Vision; es wird noch nicht umgesetzt.

## Ziel & Mehrwert

Der größte Mehrwert des Auftragsmanagements ist, **alle Änderungen an einem Zug nachvollziehbar zu
machen**. Ein „Zug" ist über die Zeit nicht stabil: er fächert sich nach Verkehrstagen auf, ändert
zeitweise Fahrzeug oder Fahrzeiten (Ferien, Baustellen), und selbst die OTN kann wechseln. Trotzdem
ist es derselbe Zug und dieselbe Auftragsposition. Der Planer muss diesen Zug **über die alte/bekannte
OTN wiederfinden** und die komplette Änderungshistorie sehen.

## TTT-Spec-Grundlage (verifiziert)

- **Stabile Identität = ReferenceTrainID (TRIDv00, Variant immer „00")** — nicht die OTN. Quelle:
  `docs/private/ttt-schnittstellenspezifikation.md` (ReferenceTrainID), `ttt-anlage1-datenfelder.md`.
- **Ein Referenzzug → mehrere Paths**, je mit eigenem `PlannedCalendar` (Teilmenge des
  `ReferenceTrainIDSubCalendar`). Constraint: *„nicht zwei Paths mit gleichem TRIDv00 gleichzeitig"*
  → die Ausprägungen eines Zuges haben **disjunkte Verkehrstage**. Jeder Path = **eine** TTT-Bestellung.
- **OTN** wird je Path zugewiesen (`OperationalTrainNumber`) und ist **nicht stabil**; die „Änderung der
  mit einer Trasse verknüpften Reference Train ID" ist ein dokumentierter Änderungstyp.
- **Modification (selbst) vs. Alteration (Infrastruktur)** sind im Fluss vorgesehen.

## Drei-Ebenen-Modell

```
Auftragsposition  (Zug-Identität, stabil, findbar über alte OTN/TRID)
   │  name, TRID, aktuelle OTN + OTN-Historie, Typ FAHRPLAN
   │
   ├─ Ausprägung 1  (Kind = OTN × Verkehrstage-Kalender)        → 1 TTT-Bestellung
   │     Mo–Fr · OTN 18001 · 2×Flirt · A→B→C · ab 08:12
   │     ├─ Version (Basis)        Flirt, gültig Mo–Fr ganzjährig
   │     └─ Version (Override)     Traverso, gültig KW 28–31  (Modification, selbst)
   │
   └─ Ausprägung 2  (Kind)                                       → 1 TTT-Bestellung
         Sa–So · OTN 18051 · 1×Flirt · A→C · ab 08:30
         └─ Version (Override)     Ankunft Chur 09:47→09:52, gültig 12.10.  (Alteration, Infra)
```

### Ebene 1 — Auftragsposition (Zug-Identität)
- Stabiles internes `UUID` (= heutige `OrderPosition.id`), **dauerhaft**.
- **Identitäts-Anker = TRID**; **OTN = menschliches Label mit Historie** (find-key bleibt die alte OTN).
- Findbar über: aktuelle OTN, **jede historische OTN**, TRID, Name.
- Wird vom Container/Eltern getragen — die einzelne Fahrt-Konfiguration liegt in den Kindern.

### Ebene 2 — Ausprägung (Kind) = OTN × Verkehrstage-Kalender
- Genau **ein disjunktes Verkehrstage-Muster** (Mo–Fr, Sa–So, …) mit eigener Route/Zeitlage/Fahrzeug
  und eigener OTN.
- Bildet **1:1 auf eine TTT-Bestellung** ab (`PurchasePosition` mit `pmPathRequestId` hängt hier).
- **Regel (aus TTT):** Kinder desselben Zuges haben **nicht überlappende** Verkehrstage → beim
  Anlegen/Spiegeln validieren.

### Ebene 3 — Version (Änderungsstrang je Kind)
- Eine Version = **zeitlich begrenzter Override** eines Kindes für einen Teil seines Kalenders:
  geänderte Zeiten, anderes Fahrzeug (Ferien-Traverso), geänderte Route (Baustelle).
- Trägt: Gültigkeits-Teilbereich (welche Tage), Quelle `MODIFICATION` (selbst) | `ALTERATION` (Infra),
  geänderte Attribute, Versionsnummer, Zeitstempel, Auslöser.
- **Effektive Konfiguration an einem Tag** = die Version, deren Gültigkeit diesen Tag abdeckt, sonst die
  Basisversion (bitemporal: „welche Ausprägung gilt an Tag X").
- Antwort auf die Abgrenzungsfrage: **anderes Verkehrstage-Muster ⇒ neues Kind; gleiche Verkehrstage,
  geänderte Zeit/Fahrzeug ⇒ neue Version** (mit eigenem Gültigkeitsfenster).

## Mapping auf den heutigen Code

| Konzept-Ebene | Heute vorhanden | Lücke |
|---|---|---|
| Zug-Identität | `OrderPosition` (id, name, OTN, `pmReferenceTrainId`) | OTN-Historie; „ist Container" |
| Ausprägung (Kind) | `OrderPosition.variantOf`/`variantType` (**vorhanden, ungenutzt**), `validity`, `purchasePositions` | Aktivieren; Disjunktheits-Check; OTN je Kind |
| Version | RailOpt: `PmTrainVersion` (`INITIAL/MODIFICATION/ALTERATION/CANCELLATION`) + `PmJourneyLocation` | Order-Mgmt-seitiger Versions-/Override-Strang mit Gültigkeitsfenster |
| Änderungs-Ausweis | `DeviationDetector` (Slice J, vergleicht INITIAL↔latest) | Generalisieren auf Versions-Timeline + Quelle |
| Kalender | `ValidityJsonCodec`, `PlannedCalendar`/Bitmap aus RailOpt | Verkehrstage-Bitmap je Kind/Version |

**Wichtig:** `variantOf`/`variantType` liegen als totes Schema-Gerüst bereits vor — die Eltern-Kind-
Hierarchie kann darauf aufsetzen statt neu erfunden zu werden.

## Änderungen ausweisen (Kern-Feature)

- **Änderungs-Feed je Zug:** chronologische Liste über alle Kinder/Versionen:
  „KW 28–31: Fahrzeug Flirt→Traverso (Modification, du)", „12.10.: Ankunft Chur 09:47→09:52
  (Alteration, Infra)", „03/26: OTN 18001→18077".
- **⚠-Badge** je Kind/Zug bei offenen (nicht akzeptierten) Alterations oder nicht synchronisierten
  Modifications — der Slice-J-Badge ist der Keim, wird auf den Versions-Vergleich + Quelle erweitert.
- **OTN-Historie** als Chip am Zug-Header („aktuell 18077 · war 18001 bis 03/26").
- Akzeptieren/Ablehnen von Alterations: bestehender Alteration-Flow (`PurchaseOrderService`,
  `respondToAlteration`) wird je Kind/Version wiederverwendet.

## UI-Skizze

```
┌ IC 93500 · TRID 1188/…/00 ────────────────────────────────────┐
│ OTN aktuell 93500 · war 18001 (bis 03/26)      [⚠ 1 Alteration]│
│                                                                │
│ Ausprägungen                                                   │
│ ▸ Mo–Fr · OTN 93500 · 2×Flirt · A→B→C · 08:12   ●3 best ○1 off │  ← PurchaseStatusRollup
│ ▾ Sa–So · OTN 93551 · 1×Flirt · A→C · 08:30     ●2 best  [⚠]   │
│     Versionen:                                                 │
│       Basis      Flirt        Sa–So ganzjährig                 │
│       Override   +5min Ankunft 12.10.   (Alteration)  [✓][✗]   │
│ [+ Ausprägung]                                                 │
│                                                                │
│ [Änderungen] ← Feed über den ganzen Zug                        │
└────────────────────────────────────────────────────────────────┘
```

Baut auf bestehenden Bausteinen: `OrderPositionPanel`/`OrderPositionRow` (Kompakt/Aufklappen,
Auswahl), `PurchaseStatusRollup`, `DeviationDetector`, Alteration-Buttons.

## Inkrementeller Bau-Pfad (rückwärtskompatibel)

1. **Datenmodell:** `variantOf`-Hierarchie aktivieren; `PositionVariantType`-Enum
   (`ZUG`/`AUSPRAEGUNG`); Order-Mgmt-`OrderPositionVersion` (oder Wiederverwendung `PmTrainVersion`-
   Spiegel) mit Gültigkeitsfenster + Quelle; `otn_history`-Tabelle. Flyway-Migration + Audit.
2. **RailOpt-Spiegel:** beim Capture/Sync eines ReferenceTrains mit mehreren SubCalendars/Paths
   automatisch Eltern + Kinder anlegen (User-Entscheid: „aus RailOpt, manuell ergänzbar").
3. **Manuelles Anlegen** einer Ausprägung im UI (mit Disjunktheits-Check der Verkehrstage).
4. **Versions-Timeline + Änderungs-Feed** (DeviationDetector generalisieren; Quelle Modification/
   Alteration; effektive-Konfiguration-Auflösung pro Tag).
5. **Migration Bestand:** heutige flache FAHRPLAN-Position = Zug mit **genau einem** Kind
   (degenerierter Fall), damit alte Daten unverändert rendern.

## Offene Detailfragen (für die Bau-Phase, nicht jetzt blockierend)

- Verkehrstage-Repräsentation: Bitmap (wie RailOpt `calendarBitmap`) vs. explizite Tagesliste
  (`ValidityJsonCodec`)? Empfehlung: Bitmap je Fahrplanjahr, kompatibel zu RailOpt.
- Version = eigenes Entity vs. „Kind mit Gültigkeitsfenster unter dem Kind"? Empfehlung: eigenes
  leichtes Version-Entity je Kind (klarer Override-Strang, einfacher Feed).
- Wie tief wird die Ressourcen-/Fahrzeug-Bindung je Version abgebildet (nur Anzeige vs. eigener
  `ResourceNeed`)? Hängt am Fahrzeug-Stammdaten-Ausbau.
