-- V43: Configurable deadline rules (Frist-Regeln, K2-S5). A rule selects member positions, anchors
-- a deadline (absolute or rolling ±N days), and carries a trigger + action. Seeds three example
-- rules (one fixed Fahrplanjahr cut-off, two rolling around the trip). Mock demo data.

CREATE TABLE frist_regeln (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    member_filter       VARCHAR(30)  NOT NULL,
    anchor              VARCHAR(30)  NOT NULL,
    absolute_date       DATE,
    offset_days         INTEGER,
    warn_threshold_days INTEGER,
    trigger_type        VARCHAR(20)  NOT NULL,
    trigger_status      VARCHAR(40),
    action              VARCHAR(20)  NOT NULL,
    enabled             BOOLEAN      NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE frist_regeln_audit (
    id                  UUID         NOT NULL,
    name                VARCHAR(255),
    member_filter       VARCHAR(30),
    anchor              VARCHAR(30),
    absolute_date       DATE,
    offset_days         INTEGER,
    warn_threshold_days INTEGER,
    trigger_type        VARCHAR(20),
    trigger_status      VARCHAR(40),
    action              VARCHAR(20),
    enabled             BOOLEAN,
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT,
    revision_id         INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type       SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);

INSERT INTO frist_regeln
  (id, name, member_filter, anchor, absolute_date, offset_days, warn_threshold_days,
   trigger_type, trigger_status, action, enabled)
VALUES
  ('f0000001-0000-4000-8000-000000000001', 'Final-Offer Jahresfahrplan 2027',
   'NICHT_BESTELLT', 'ABSOLUT', '2026-12-01', 0, 30, 'DATUM', NULL, 'ANZEIGEN', true),
  ('f0000001-0000-4000-8000-000000000002', 'Fahrzeugzuweisung 2 Tage vor Fahrt',
   'ALLE_FAHRPLAN', 'FAHRT', NULL, -2, 5, 'DATUM', NULL, 'ANZEIGEN', true),
  ('f0000001-0000-4000-8000-000000000003', 'Verrechnung 10 Tage nach Fahrt',
   'ALLE_FAHRPLAN', 'FAHRT', NULL, 10, 7, 'DATUM', NULL, 'ANZEIGEN', true);
