-- V14: Seed additional timetable years for TTR phase demonstration
-- (2026 already exists from V9)

INSERT INTO pm_timetable_years (id, year, label, start_date, end_date, created_at, updated_at, version)
VALUES
  (gen_random_uuid(), 2025, 'Fahrplanjahr 2025', '2024-12-15', '2025-12-13', now(), now(), 0),
  (gen_random_uuid(), 2027, 'Fahrplanjahr 2027', '2026-12-13', '2027-12-11', now(), now(), 0)
ON CONFLICT (year) DO NOTHING;
