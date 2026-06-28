-- V48: Automatic businesses materialized from deadline rules (Frist-Regeln, K2 vision). A FristRegel
-- can drive a Business whose membership (n:m order positions) and due date are recomputed from the
-- rule by AutoBusinessService. These two columns mark and link such auto-managed businesses. They
-- are NOT audited (derived/config state, same rationale as auto_order_log), so no audit columns.

ALTER TABLE businesses ADD COLUMN IF NOT EXISTS automatic BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS frist_regel_id UUID;

CREATE INDEX IF NOT EXISTS idx_businesses_frist_regel_id ON businesses (frist_regel_id);
