-- V42: Verkehrstage weekday-set per expression (K1-S4) — comma-separated ISO weekday numbers
-- (1=Mon..7=Sun). Backs the expression disjointness check and the per-weekday deadline rules (K2).

ALTER TABLE order_positions ADD COLUMN weekdays VARCHAR(20);
ALTER TABLE order_positions_audit ADD COLUMN weekdays VARCHAR(20);

-- Backfill the seeded demo expressions (V40): Mo–Fr and Sa–So.
UPDATE order_positions SET weekdays = '1,2,3,4,5'
 WHERE id = 'e9000003-0000-4000-8000-000000000001';
UPDATE order_positions SET weekdays = '6,7'
 WHERE id = 'e9000003-0000-4000-8000-000000000002';
