-- V38: Seed a self-contained deviation demo (slice J) so the ⚠ deviation badge is visible without
-- relying on any runtime capture. One order position links to a RailOpt reference train and
-- deviates on two axes:
--   * order vs. RailOpt: position destination "Zürich HB" ≠ train v2 last stop "Zug", and the
--     position start 2026-08-15 ≠ the train calendar_start 2026-09-01.
--   * version vs. original: the train has an INITIAL v1 and a MODIFICATION v2 whose stop times
--     were shifted, so the latest version drifts from the original.
-- Mock demo data only.

INSERT INTO orders
  (id, order_number, name, valid_from, valid_to, process_status, cost_center,
   created_at, updated_at, version)
VALUES
  ('d8000001-0000-4000-8000-000000000001', 'ABW-2026-001', 'Abweichungs-Demo — IC 93500',
   '2026-09-01', '2026-12-12', 'AUFTRAG', 'PSP-2026-ABW',
   now(), now(), 0);

-- Position deviates from the linked train on destination + start date.
INSERT INTO order_positions
  (id, order_id, name, type, from_location, to_location, operational_train_number,
   start, "end", pm_reference_train_id, created_at, updated_at, version)
VALUES
  ('d8000002-0000-4000-8000-000000000001', 'd8000001-0000-4000-8000-000000000001',
   'IC 93500', 'FAHRPLAN', 'Luzern', 'Zürich HB', '93500',
   TIMESTAMP '2026-08-15 12:05:00', TIMESTAMP '2026-12-12 12:28:00',
   'd8000003-0000-4000-8000-000000000001', now(), now(), 0);

-- Reference train, back-linked to the position (source_position_id) so it counts as assigned.
INSERT INTO pm_reference_trains
  (id, timetable_year_id, trid_company, trid_core, trid_variant, trid_timetable_year,
   operational_train_number, calendar_start, calendar_end, process_state, planning_status,
   source_position_id)
VALUES
  ('d8000003-0000-4000-8000-000000000001',
   (SELECT id FROM pm_timetable_years WHERE year = 2026),
   '1188', 'DEVIATION-93500', '01', 2026, '93500', '2026-09-01', '2026-12-12', 'BOOKED', 'PLANNED',
   'd8000002-0000-4000-8000-000000000001');

INSERT INTO pm_train_versions
  (id, reference_train_id, version_number, version_type,
   operational_train_number, calendar_start, calendar_end)
VALUES
  ('d8000004-0000-4000-8000-000000000001', 'd8000003-0000-4000-8000-000000000001', 1, 'INITIAL',
   '93500', '2026-09-01', '2026-12-12'),
  ('d8000004-0000-4000-8000-000000000002', 'd8000003-0000-4000-8000-000000000001', 2, 'MODIFICATION',
   '93500', '2026-09-01', '2026-12-12');

INSERT INTO pm_journey_locations
  (train_version_id, sequence, primary_location_name, uopid, journey_location_type,
   departure_time, arrival_time)
VALUES
  -- v1 (original)
  ('d8000004-0000-4000-8000-000000000001', 1, 'Luzern', 'CH85700', '01', '12:00', NULL),
  ('d8000004-0000-4000-8000-000000000001', 2, 'Zug',    'CH85703', '03', NULL,   '12:20'),
  -- v2 (modified: both times shifted)
  ('d8000004-0000-4000-8000-000000000002', 1, 'Luzern', 'CH85700', '01', '12:05', NULL),
  ('d8000004-0000-4000-8000-000000000002', 2, 'Zug',    'CH85703', '03', NULL,   '12:28');
