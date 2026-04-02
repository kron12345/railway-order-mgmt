# Domain Glossary — Railway Order Management

| Deutsch | English | Italiano | Français | Description |
|---|---|---|---|---|
| Auftrag | Order | Ordine | Commande | A transport order for goods or materials |
| Auftraggeber | Customer | Cliente | Client | Entity placing the transport order |
| Wagen / Waggon | Railcar / Wagon | Carro / Vagone | Wagon | Railway vehicle for freight transport |
| Strecke | Route | Percorso | Itinéraire | Path between origin and destination |
| Abfahrtsbahnhof | Origin Station | Stazione di Partenza | Gare de Départ | Starting station of a transport |
| Zielbahnhof | Destination Station | Stazione di Arrivo | Gare d'Arrivée | End station of a transport |
| Ladung | Cargo / Freight | Carico | Chargement | Goods being transported |
| Frachtbrief | Waybill | Lettera di Vettura | Lettre de Voiture | Transport document accompanying the goods |
| Disponierung | Dispatching | Smistamento | Dispatching | Planning and coordination of transport orders |
| Zugbildung | Train Formation | Formazione Treno | Formation du Train | Assembling wagons into a train |
| Fahrplan | Schedule / Timetable | Orario | Horaire | Planned departure and arrival times |
| Gleisanlage | Track Layout | Impianto Binari | Installation de Voies | Physical track infrastructure |
| Rangierbahnhof | Marshalling Yard | Scalo di Smistamento | Gare de Triage | Yard for sorting and assembling trains |
| Ladeeinheit | Loading Unit | Unità di Carico | Unité de Chargement | Container, swap body, or similar unit |
| Gefahrgut | Dangerous Goods | Merci Pericolose | Marchandises Dangereuses | Hazardous materials requiring special handling |
| Zeitverschiebung (Shift) | Shift Propagation | Propagazione a Spostamento | Propagation par Décalage | Time propagation mode where all following stops are moved by the same delta until the next pinned stop |
| Zeitstreckung (Stretch) | Stretch Propagation | Propagazione Proporzionale | Propagation par Étirement | Time propagation mode where travel times between two pins are scaled proportionally |
| Pin (Zeitanker) | Pin (Time Anchor) | Pin (Ancora Temporale) | Pin (Ancre Temporelle) | A row marked as pinned acts as an immovable anchor for shift/stretch propagation — its times are never modified by propagation |
| Kommerzielle Zeit | Commercial Time | Orario Commerciale | Horaire Commercial | Published timetable time (PLA/PLD) as shown in public timetables, may differ from operational time |
| TimingQualifierCode | TimingQualifierCode | TimingQualifierCode | TimingQualifierCode | TTT standard code qualifying a time value: ALA/ALD (exact), ELA/LLA/ELD/LLD (window), PLA/PLD (commercial) |
| Zugaktivitaet | Train Activity (TrainActivityType) | Attività Treno | Activité du Train | TTT activity code describing the reason for a stop (e.g., 0001 = boarding, 0012 = loco change, 0040 = pass-through) |
| Fahrplanmanager | Path Manager | Gestore Tracce | Gestionnaire de Sillons | Module managing the TTT path request lifecycle — simulates RA/IM communication for timetable path ordering |
| Referenzzug | Reference Train | Treno di Riferimento | Train de Référence | Central aggregate in the Path Manager, identified by a TRID (Company + Core + Variant + Timetable Year), e.g. `SOB0-000042-01-2026` |
| Trasse / Pfad | Path (PAID) | Traccia | Sillon | A path allocated by the Infrastructure Manager, identified by a PAID (Path Allocation Identifier) |
| Trassenantrag | Path Request (PRID) | Richiesta di Traccia | Demande de Sillon | A formal request for a path, identified by a PRID (Path Request Identifier) |
| Laufweg | Route (ROID) | Percorso | Itinéraire | Ordered sequence of operational points defining the geographical route, identified by a ROID (Route Identifier) |
| Zugversion | Train Version | Versione del Treno | Version du Train | Immutable snapshot of a train's data (header + journey locations) at a specific point in the path lifecycle. Types: INITIAL, MODIFICATION, ALTERATION, CANCELLATION |
| Prozessschritt | Process Step | Passo di Processo | Étape de Processus | Immutable audit record of a single state transition in the TTT lifecycle, recording action, from-state, to-state, comment, and timestamp |
| State Machine (TTT) | State Machine (TTT) | Macchina a Stati (TTT) | Machine à États (TTT) | The PathProcessEngine implements a static transition table governing the lifecycle of a path request from NEW through BOOKED to terminal states (CANCELED, WITHDRAWN, NO_ALTERNATIVE) |
| RA (Verantwortlicher Antragsteller) | RA (Responsible Applicant) | RA (Richiedente Responsabile) | RA (Demandeur Responsable) | The railway undertaking or entity requesting a train path from the Infrastructure Manager |
| IM (Infrastrukturbetreiber / Planungs-IM) | IM (Infrastructure Manager / Planning IM) | IM (Gestore dell'Infrastruttura) | IM (Gestionnaire d'Infrastructure) | The entity managing railway infrastructure and allocating paths — in the simulation, IM actions are triggered manually by the user |
| Vorangebot | Draft Offer | Offerta Preliminare | Offre Préliminaire | A preliminary path offer from the IM that may differ from the original request — creates a new train version for comparison |
| Endgueltiges Angebot | Final Offer | Offerta Definitiva | Offre Finale | A binding path offer from the IM that can be accepted to book the path — creates a new train version |
| Gebucht | Booked | Prenotato | Réservé | The path is confirmed and entered into the timetable — the train can now run on the allocated path |
| Fahrplanjahr | Timetable Year | Anno di Orario | Année Horaire | The railway timetable period, typically running from mid-December to mid-December of the following year (e.g., 2025-12-14 to 2026-12-12 for timetable year 2026) |
| TRID | TRID (Train Request ID) | TRID | TRID | Composite identifier for a reference train: Company (4 chars) + Core (20 chars) + Variant (2 chars) + Timetable Year |
| PAID | PAID (Path Allocation ID) | PAID | PAID | Composite identifier for an allocated path, structured like TRID |
| PRID | PRID (Path Request ID) | PRID | PRID | Composite identifier for a path request, structured like TRID |
| ROID | ROID (Route ID) | ROID | ROID | Composite identifier for a route, structured like TRID |
| Bestellkalender | Purchase Calendar | Calendario Ordini | Calendrier de Commandes | UI panel for managing capacity orders (purchase positions) per order position, organized by TTR phases (X-11, X-8, X-5, Ad-hoc) |
| Benutzerrolle | User Role | Ruolo Utente | Role Utilisateur | One of three application roles assigned via Keycloak: ADMIN (full access incl. settings), DISPATCHER (order and timetable management), VIEWER (read-only access) |
| Theme | Theme | Tema | Theme | Visual appearance of the application UI — available options: Dark Amber, Dark Teal, Light. Stored per user profile and applied immediately on change |
| Audit Trail | Audit Trail | Traccia di Audit | Piste d'Audit | Automatic history of all entity changes recorded by Hibernate Envers. Tracks who changed what and when for orders, positions, resources, archives, and path manager entities |
| Filter | Filter | Filtro | Filtre | UI mechanism for narrowing down displayed data — used in the order list (by status, text search) and in the accordion view (position type filter within an expanded order) |
