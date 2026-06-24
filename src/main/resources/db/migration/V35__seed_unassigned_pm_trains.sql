-- V35: Seed unassigned RailOpt reference trains (source_position_id IS NULL) so the slice-D demo
-- "capture an unassigned Fahrplan as an order position" has data. Each carries one INITIAL version
-- with origin/destination journey locations, so the captured position shows OTN, von/nach and
-- validity. Mock data only.

INSERT INTO pm_reference_trains
  (id, timetable_year_id, trid_company, trid_core, trid_variant, trid_timetable_year,
   operational_train_number, calendar_start, calendar_end, process_state, planning_status)
VALUES
  ('a5500001-0000-4000-8000-000000000001',
   (SELECT id FROM pm_timetable_years WHERE year = 2026),
   '1188', 'UNASSIGN-90201', '01', 2026, '90201', '2026-06-01', '2026-12-12', 'NEW', 'UNPLANNED'),
  ('a5500001-0000-4000-8000-000000000002',
   (SELECT id FROM pm_timetable_years WHERE year = 2026),
   '1188', 'UNASSIGN-90455', '01', 2026, '90455', '2026-07-01', '2026-12-12', 'NEW', 'UNPLANNED'),
  ('a5500001-0000-4000-8000-000000000003',
   (SELECT id FROM pm_timetable_years WHERE year = 2026),
   '1188', 'UNASSIGN-91020', '01', 2026, '91020', '2026-08-15', '2026-12-12', 'NEW', 'UNPLANNED');

INSERT INTO pm_train_versions
  (id, reference_train_id, version_number, version_type,
   operational_train_number, calendar_start, calendar_end)
VALUES
  ('b5500001-0000-4000-8000-000000000001', 'a5500001-0000-4000-8000-000000000001', 1, 'INITIAL',
   '90201', '2026-06-01', '2026-12-12'),
  ('b5500001-0000-4000-8000-000000000002', 'a5500001-0000-4000-8000-000000000002', 1, 'INITIAL',
   '90455', '2026-07-01', '2026-12-12'),
  ('b5500001-0000-4000-8000-000000000003', 'a5500001-0000-4000-8000-000000000003', 1, 'INITIAL',
   '91020', '2026-08-15', '2026-12-12');

INSERT INTO pm_journey_locations
  (train_version_id, sequence, primary_location_name, uopid, journey_location_type,
   departure_time, arrival_time)
VALUES
  ('b5500001-0000-4000-8000-000000000001', 1, 'St. Gallen',  'CH85726', '01', '08:12', NULL),
  ('b5500001-0000-4000-8000-000000000001', 2, 'Chur',        'CH85063', '03', NULL,   '09:47'),
  ('b5500001-0000-4000-8000-000000000002', 1, 'Bern',        'CH85000', '01', '10:04', NULL),
  ('b5500001-0000-4000-8000-000000000002', 2, 'Luzern',      'CH85700', '03', NULL,   '11:01'),
  ('b5500001-0000-4000-8000-000000000003', 1, 'Arth-Goldau', 'CH85180', '01', '14:20', NULL),
  ('b5500001-0000-4000-8000-000000000003', 2, 'Biasca',      'CH85400', '03', NULL,   '15:55');
