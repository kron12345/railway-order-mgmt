-- V40: Seed a train (ZUG identity) with two expressions (Ausprägungen), so the Zug → Ausprägung
-- hierarchy (K1-S2) is visible: a Mo–Fr 2×Flirt child and a Sa–So 1×Flirt child under one identity.
-- Mock demo data only.

INSERT INTO orders
  (id, order_number, name, valid_from, valid_to, process_status, cost_center,
   created_at, updated_at, version)
VALUES
  ('e9000001-0000-4000-8000-000000000001', 'ZUG-2026-001', 'Zug-Demo — S2 Linie',
   '2026-06-01', '2026-12-12', 'AUFTRAG', 'PSP-2026-ZUG', now(), now(), 0);

-- Train identity (ZUG): stable, carries the OTN label; the expressions below hold the bookings.
INSERT INTO order_positions
  (id, order_id, name, type, operational_train_number, variant_type,
   from_location, to_location, created_at, updated_at, version)
VALUES
  ('e9000002-0000-4000-8000-000000000001', 'e9000001-0000-4000-8000-000000000001',
   'S2 18000', 'FAHRPLAN', '18000', 'ZUG', 'Zürich HB', 'Zug', now(), now(), 0);

-- Expressions (AUSPRAEGUNG) = OTN × Verkehrstage, each a separate TTT booking.
INSERT INTO order_positions
  (id, order_id, name, type, operational_train_number, variant_type, variant_of_id,
   from_location, to_location, service_type, start, "end", created_at, updated_at, version)
VALUES
  ('e9000003-0000-4000-8000-000000000001', 'e9000001-0000-4000-8000-000000000001',
   'Mo–Fr · 2×Flirt', 'FAHRPLAN', '18001', 'AUSPRAEGUNG', 'e9000002-0000-4000-8000-000000000001',
   'Zürich HB', 'Zug', '2×Flirt',
   TIMESTAMP '2026-06-01 08:12:00', TIMESTAMP '2026-12-11 09:47:00', now(), now(), 0),
  ('e9000003-0000-4000-8000-000000000002', 'e9000001-0000-4000-8000-000000000001',
   'Sa–So · 1×Flirt', 'FAHRPLAN', '18051', 'AUSPRAEGUNG', 'e9000002-0000-4000-8000-000000000001',
   'Zürich HB', 'Zug', '1×Flirt',
   TIMESTAMP '2026-06-06 08:30:00', TIMESTAMP '2026-12-12 10:01:00', now(), now(), 0);
