-- V36: Inbound R2P order intake inbox (slice E). Staging table for orders that arrive via R2P
-- (a third party sends a timetable + a personnel/vehicle request) before a planner accepts them.
-- Mock intake staging; not audited.

CREATE TABLE r2p_inbox_entries (
    id                       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    requester                VARCHAR(255),
    operational_train_number VARCHAR(20),
    from_location            VARCHAR(255),
    to_location              VARCHAR(255),
    start_at                 TIMESTAMP,
    end_at                   TIMESTAMP,
    status                   VARCHAR(20) NOT NULL DEFAULT 'EINGEGANGEN',
    linked_position_id       UUID,
    requested_resources_json JSONB,
    received_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_r2p_inbox_status ON r2p_inbox_entries (status);
