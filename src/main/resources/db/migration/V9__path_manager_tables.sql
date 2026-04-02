-- V9: Path Manager domain tables for TTT (TSI TAF/TAP) integration

-- 1. Timetable years
CREATE TABLE pm_timetable_years (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    year        INT         NOT NULL UNIQUE,
    label       VARCHAR(100),
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE pm_timetable_years_aud (
    id          UUID        NOT NULL,
    year        INT,
    label       VARCHAR(100),
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    version     BIGINT,
    revision_id INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type SMALLINT  NOT NULL,
    PRIMARY KEY (id, revision_id)
);

-- 2. Reference trains
CREATE TABLE pm_reference_trains (
    id                       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    timetable_year_id        UUID        NOT NULL REFERENCES pm_timetable_years(id),
    trid_company             VARCHAR(4)  NOT NULL,
    trid_core                VARCHAR(20) NOT NULL,
    trid_variant             VARCHAR(2)  NOT NULL DEFAULT '01',
    trid_timetable_year      INT         NOT NULL,
    operational_train_number VARCHAR(20),
    train_type               VARCHAR(2),
    traffic_type_code        VARCHAR(10),
    push_pull_train          BOOLEAN     DEFAULT FALSE,
    calendar_start           DATE,
    calendar_end             DATE,
    calendar_bitmap          TEXT,
    train_weight             INT,
    train_length             INT,
    train_max_speed          INT,
    brake_type               VARCHAR(10),
    source_position_id       UUID,
    process_state            VARCHAR(30) NOT NULL DEFAULT 'NEW',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                  BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE pm_reference_trains_aud (
    id                       UUID        NOT NULL,
    timetable_year_id        UUID,
    trid_company             VARCHAR(4),
    trid_core                VARCHAR(20),
    trid_variant             VARCHAR(2),
    trid_timetable_year      INT,
    operational_train_number VARCHAR(20),
    train_type               VARCHAR(2),
    traffic_type_code        VARCHAR(10),
    push_pull_train          BOOLEAN,
    calendar_start           DATE,
    calendar_end             DATE,
    calendar_bitmap          TEXT,
    train_weight             INT,
    train_length             INT,
    train_max_speed          INT,
    brake_type               VARCHAR(10),
    source_position_id       UUID,
    process_state            VARCHAR(30),
    created_at               TIMESTAMPTZ,
    updated_at               TIMESTAMPTZ,
    version                  BIGINT,
    revision_id              INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type            SMALLINT    NOT NULL,
    PRIMARY KEY (id, revision_id)
);

CREATE INDEX idx_pm_reference_trains_year ON pm_reference_trains(timetable_year_id);
CREATE INDEX idx_pm_reference_trains_state ON pm_reference_trains(process_state);

-- 3. Routes
CREATE TABLE pm_routes (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_train_id  UUID        NOT NULL REFERENCES pm_reference_trains(id),
    roid_company        VARCHAR(4)  NOT NULL,
    roid_core           VARCHAR(20) NOT NULL,
    roid_variant        VARCHAR(2)  NOT NULL DEFAULT '01',
    roid_timetable_year INT         NOT NULL,
    route_points        JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE pm_routes_aud (
    id                  UUID        NOT NULL,
    reference_train_id  UUID,
    roid_company        VARCHAR(4),
    roid_core           VARCHAR(20),
    roid_variant        VARCHAR(2),
    roid_timetable_year INT,
    route_points        JSONB,
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ,
    version             BIGINT,
    revision_id         INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type       SMALLINT    NOT NULL,
    PRIMARY KEY (id, revision_id)
);

CREATE INDEX idx_pm_routes_train ON pm_routes(reference_train_id);

-- 4. Path requests
CREATE TABLE pm_path_requests (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_train_id  UUID        NOT NULL REFERENCES pm_reference_trains(id),
    route_id            UUID        REFERENCES pm_routes(id),
    prid_company        VARCHAR(4)  NOT NULL,
    prid_core           VARCHAR(20) NOT NULL,
    prid_variant        VARCHAR(2)  NOT NULL DEFAULT '01',
    prid_timetable_year INT         NOT NULL,
    type_of_request     INT,
    process_type        INT,
    message_status      INT,
    current_state       VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE pm_path_requests_aud (
    id                  UUID        NOT NULL,
    reference_train_id  UUID,
    route_id            UUID,
    prid_company        VARCHAR(4),
    prid_core           VARCHAR(20),
    prid_variant        VARCHAR(2),
    prid_timetable_year INT,
    type_of_request     INT,
    process_type        INT,
    message_status      INT,
    current_state       VARCHAR(30),
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ,
    version             BIGINT,
    revision_id         INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type       SMALLINT    NOT NULL,
    PRIMARY KEY (id, revision_id)
);

CREATE INDEX idx_pm_path_requests_train ON pm_path_requests(reference_train_id);
CREATE INDEX idx_pm_path_requests_route ON pm_path_requests(route_id);

-- 5. Paths (offers/bookings)
CREATE TABLE pm_paths (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    path_request_id     UUID        REFERENCES pm_path_requests(id),
    reference_train_id  UUID        NOT NULL REFERENCES pm_reference_trains(id),
    paid_company        VARCHAR(4)  NOT NULL,
    paid_core           VARCHAR(20) NOT NULL,
    paid_variant        VARCHAR(2)  NOT NULL DEFAULT '01',
    paid_timetable_year INT         NOT NULL,
    current_state       VARCHAR(30) NOT NULL DEFAULT 'DRAFT_OFFER',
    type_of_information INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE pm_paths_aud (
    id                  UUID        NOT NULL,
    path_request_id     UUID,
    reference_train_id  UUID,
    paid_company        VARCHAR(4),
    paid_core           VARCHAR(20),
    paid_variant        VARCHAR(2),
    paid_timetable_year INT,
    current_state       VARCHAR(30),
    type_of_information INT,
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ,
    version             BIGINT,
    revision_id         INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type       SMALLINT    NOT NULL,
    PRIMARY KEY (id, revision_id)
);

CREATE INDEX idx_pm_paths_request ON pm_paths(path_request_id);
CREATE INDEX idx_pm_paths_train ON pm_paths(reference_train_id);

-- 6. Train versions (snapshots of train data per process step)
CREATE TABLE pm_train_versions (
    id                       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_train_id       UUID        NOT NULL REFERENCES pm_reference_trains(id),
    path_id                  UUID        REFERENCES pm_paths(id),
    version_number           INT         NOT NULL,
    version_type             VARCHAR(20) NOT NULL,
    label                    VARCHAR(255),
    operational_train_number VARCHAR(20),
    train_type               VARCHAR(2),
    traffic_type_code        VARCHAR(10),
    train_weight             INT,
    train_length             INT,
    train_max_speed          INT,
    calendar_start           DATE,
    calendar_end             DATE,
    calendar_bitmap          TEXT,
    offset_to_reference      INT         DEFAULT 0,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                  BIGINT      NOT NULL DEFAULT 0,
    UNIQUE (reference_train_id, version_number)
);

CREATE TABLE pm_train_versions_aud (
    id                       UUID        NOT NULL,
    reference_train_id       UUID,
    path_id                  UUID,
    version_number           INT,
    version_type             VARCHAR(20),
    label                    VARCHAR(255),
    operational_train_number VARCHAR(20),
    train_type               VARCHAR(2),
    traffic_type_code        VARCHAR(10),
    train_weight             INT,
    train_length             INT,
    train_max_speed          INT,
    calendar_start           DATE,
    calendar_end             DATE,
    calendar_bitmap          TEXT,
    offset_to_reference      INT,
    created_at               TIMESTAMPTZ,
    updated_at               TIMESTAMPTZ,
    version                  BIGINT,
    revision_id              INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type            SMALLINT    NOT NULL,
    PRIMARY KEY (id, revision_id)
);

CREATE INDEX idx_pm_train_versions_train ON pm_train_versions(reference_train_id);
CREATE INDEX idx_pm_train_versions_path ON pm_train_versions(path_id);

-- 7. Journey locations (ordered stops per train version)
CREATE TABLE pm_journey_locations (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    train_version_id        UUID        NOT NULL REFERENCES pm_train_versions(id),
    sequence                INT         NOT NULL,
    country_code_iso        VARCHAR(2),
    location_primary_code   VARCHAR(10),
    primary_location_name   VARCHAR(255),
    uopid                   VARCHAR(20),
    journey_location_type   VARCHAR(2),
    arrival_time            VARCHAR(8),
    arrival_offset          INT         DEFAULT 0,
    departure_time          VARCHAR(8),
    departure_offset        INT         DEFAULT 0,
    dwell_time              INT,
    arrival_qualifier       VARCHAR(3),
    departure_qualifier     VARCHAR(3),
    subsidiary_code         VARCHAR(10),
    activities              JSONB,
    network_specific_params JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT      NOT NULL DEFAULT 0,
    UNIQUE (train_version_id, sequence)
);

CREATE TABLE pm_journey_locations_aud (
    id                      UUID        NOT NULL,
    train_version_id        UUID,
    sequence                INT,
    country_code_iso        VARCHAR(2),
    location_primary_code   VARCHAR(10),
    primary_location_name   VARCHAR(255),
    uopid                   VARCHAR(20),
    journey_location_type   VARCHAR(2),
    arrival_time            VARCHAR(8),
    arrival_offset          INT,
    departure_time          VARCHAR(8),
    departure_offset        INT,
    dwell_time              INT,
    arrival_qualifier       VARCHAR(3),
    departure_qualifier     VARCHAR(3),
    subsidiary_code         VARCHAR(10),
    activities              JSONB,
    network_specific_params JSONB,
    created_at              TIMESTAMPTZ,
    updated_at              TIMESTAMPTZ,
    version                 BIGINT,
    revision_id             INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type           SMALLINT    NOT NULL,
    PRIMARY KEY (id, revision_id)
);

CREATE INDEX idx_pm_journey_locations_version ON pm_journey_locations(train_version_id);

-- 8. Process steps (audit log — no Envers audit table needed)
CREATE TABLE pm_process_steps (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    path_request_id     UUID        REFERENCES pm_path_requests(id),
    path_id             UUID        REFERENCES pm_paths(id),
    reference_train_id  UUID        NOT NULL REFERENCES pm_reference_trains(id),
    step_type           VARCHAR(30) NOT NULL,
    from_state          VARCHAR(30),
    to_state            VARCHAR(30),
    type_of_information INT,
    message_status      INT,
    comment             VARCHAR(2000),
    simulated_by        VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pm_process_steps_train ON pm_process_steps(reference_train_id);
CREATE INDEX idx_pm_process_steps_request ON pm_process_steps(path_request_id);
CREATE INDEX idx_pm_process_steps_path ON pm_process_steps(path_id);

-- Link order positions to reference trains
ALTER TABLE order_positions
    ADD COLUMN pm_reference_train_id UUID;

ALTER TABLE order_positions_aud
    ADD COLUMN pm_reference_train_id UUID;

-- Seed initial timetable year 2026
INSERT INTO pm_timetable_years (year, label, start_date, end_date)
VALUES (2026, 'Fahrplanjahr 2026', '2025-12-14', '2026-12-12');
