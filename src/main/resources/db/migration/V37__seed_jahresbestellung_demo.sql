-- V37: Seed a Jahresbestellung demo order so the derived JAHRESBESTELLUNG badge (slice C) is
-- actually visible. created_at is ~12 months before valid_from, giving the long lead time that
-- OrderType.of() maps to the annual cycle. Mock demo data.

INSERT INTO orders
  (id, order_number, name, valid_from, valid_to, process_status, cost_center,
   created_at, updated_at, version)
VALUES
  ('c7000001-0000-4000-8000-000000000001', 'JB-2026-001', 'Jahresfahrplan 2026 — Linie S2',
   '2026-06-01', '2026-12-12', 'AUFTRAG', 'PSP-2026-S2',
   TIMESTAMPTZ '2025-06-01 09:00:00+02', now(), 0);

INSERT INTO order_positions
  (id, order_id, name, type, from_location, to_location, operational_train_number,
   created_at, updated_at, version)
VALUES
  ('c7000002-0000-4000-8000-000000000001', 'c7000001-0000-4000-8000-000000000001',
   'S2 18001', 'FAHRPLAN', 'Zürich HB', 'Zug', '18001', now(), now(), 0),
  ('c7000002-0000-4000-8000-000000000002', 'c7000001-0000-4000-8000-000000000001',
   'S2 18002', 'FAHRPLAN', 'Zug', 'Zürich HB', '18002', now(), now(), 0);
