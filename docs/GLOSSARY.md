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
