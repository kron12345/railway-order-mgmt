-- V41: Seed the change trail for the train-with-expressions demo (K1-S3): an OTN rename on the
-- identity (17000 → 18000), a self-initiated Ferien-Traverso modification on the Mo–Fr expression,
-- and an infrastructure alteration on the Sa–So expression. Mock demo data only.

INSERT INTO position_otn_history
  (order_position_id, otn, valid_from, valid_to, source, created_at)
VALUES
  ('e9000002-0000-4000-8000-000000000001', '17000', '2025-12-14', '2026-03-31', 'MODIFICATION', now()),
  ('e9000002-0000-4000-8000-000000000001', '18000', '2026-04-01', NULL, 'MODIFICATION', now());

INSERT INTO order_position_versions
  (id, order_position_id, version_number, source, valid_from, valid_to, change_summary,
   created_at, updated_at, version)
VALUES
  -- Mo–Fr expression: base + holiday Traverso override (self-initiated modification)
  ('ea000001-0000-4000-8000-000000000001', 'e9000003-0000-4000-8000-000000000001', 1, 'INITIAL',
   '2026-06-01', NULL, 'Grundfahrplan 2×Flirt', now(), now(), 0),
  ('ea000001-0000-4000-8000-000000000002', 'e9000003-0000-4000-8000-000000000001', 2, 'MODIFICATION',
   '2026-07-13', '2026-08-09', 'Ferien: Traverso statt Flirt (KW 28–31)', now(), now(), 0),
  -- Sa–So expression: base + infrastructure alteration
  ('ea000001-0000-4000-8000-000000000003', 'e9000003-0000-4000-8000-000000000002', 1, 'INITIAL',
   '2026-06-06', NULL, 'Grundfahrplan 1×Flirt', now(), now(), 0),
  ('ea000001-0000-4000-8000-000000000004', 'e9000003-0000-4000-8000-000000000002', 2, 'ALTERATION',
   '2026-10-17', '2026-10-18', 'Baustelle: Ankunft Zug +5 min', now(), now(), 0);
