-- V3: Infrastructure master data from ERA RINF
-- Operational Points and Sections of Line for timetable planning

CREATE TABLE operational_points (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uopid       VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    country     VARCHAR(3)   NOT NULL,
    op_type     INTEGER,
    taf_tap_code VARCHAR(20),
    longitude   DOUBLE PRECISION,
    latitude    DOUBLE PRECISION,
    imported_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_op_uopid ON operational_points (uopid);
CREATE INDEX idx_op_country ON operational_points (country);
CREATE INDEX idx_op_name ON operational_points (name);
CREATE INDEX idx_op_type ON operational_points (op_type);

CREATE TABLE sections_of_line (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sol_id          VARCHAR(100) NOT NULL UNIQUE,
    start_op_uopid  VARCHAR(20)  NOT NULL,
    end_op_uopid    VARCHAR(20)  NOT NULL,
    country         VARCHAR(3)   NOT NULL,
    length_meters   DOUBLE PRECISION,
    gauge           VARCHAR(10),
    max_speed       INTEGER,
    electrified     BOOLEAN,
    imported_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_sol_start ON sections_of_line (start_op_uopid);
CREATE INDEX idx_sol_end ON sections_of_line (end_op_uopid);
CREATE INDEX idx_sol_country ON sections_of_line (country);

-- Import log to track data loads
CREATE TABLE import_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source      VARCHAR(50)  NOT NULL,
    country     VARCHAR(3),
    record_count INTEGER     NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL,
    message     VARCHAR(1000),
    started_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ
);
