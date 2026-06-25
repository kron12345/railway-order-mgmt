-- V44: Auto-order idempotency log (K2-S7) + one AUTO_BESTELLEN rule to demonstrate auto-ordering.
-- The log's unique (position, rule) pair guarantees a fired auto-order is never repeated. Mock.

CREATE TABLE auto_order_log (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_position_id UUID         NOT NULL,
    frist_regel_id    UUID         NOT NULL,
    triggered_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_auto_order UNIQUE (order_position_id, frist_regel_id)
);

INSERT INTO frist_regeln
  (id, name, member_filter, anchor, absolute_date, offset_days, warn_threshold_days,
   trigger_type, trigger_status, action, enabled)
VALUES
  ('f0000001-0000-4000-8000-000000000004', 'Auto-Bestellung Fahrzeug (Fahrt erreicht)',
   'NICHT_BESTELLT', 'FAHRT', NULL, -2, 5, 'DATUM', NULL, 'AUTO_BESTELLEN', true);
