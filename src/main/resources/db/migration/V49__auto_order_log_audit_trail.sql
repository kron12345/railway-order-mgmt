-- V49: Richer audit trail for automatic deadline-rule firings. Beyond idempotency (position, rule)
-- the log now records what fired the rule, the rule name at firing time, and a human-readable
-- outcome. auto_order_log is not @Audited (it is itself the automation log), so no audit table.

ALTER TABLE auto_order_log ADD COLUMN IF NOT EXISTS trigger_type VARCHAR(20);
ALTER TABLE auto_order_log ADD COLUMN IF NOT EXISTS rule_name    VARCHAR(255);
ALTER TABLE auto_order_log ADD COLUMN IF NOT EXISTS detail       VARCHAR(500);
