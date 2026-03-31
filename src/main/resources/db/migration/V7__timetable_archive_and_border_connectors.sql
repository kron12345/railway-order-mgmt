-- V7: Timetable archive plus synthetic border connectors for CH/DE routing

CREATE TABLE timetable_archives (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timetable_number  VARCHAR(100),
    timetable_type    VARCHAR(50),
    route_summary     VARCHAR(500),
    table_data        JSONB        NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE timetable_archives_audit (
    id                UUID         NOT NULL,
    timetable_number  VARCHAR(100),
    timetable_type    VARCHAR(50),
    route_summary     VARCHAR(500),
    table_data        JSONB,
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    version           BIGINT,
    revision_id       INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type     SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);

ALTER TABLE resource_needs
    ADD CONSTRAINT fk_resource_needs_timetable
    FOREIGN KEY (linked_fahrplan_id)
    REFERENCES timetable_archives(id);

CREATE UNIQUE INDEX idx_resource_needs_capacity_per_position
    ON resource_needs (order_position_id)
    WHERE resource_type = 'CAPACITY';

CREATE UNIQUE INDEX idx_resource_needs_linked_fahrplan
    ON resource_needs (linked_fahrplan_id)
    WHERE linked_fahrplan_id IS NOT NULL;

INSERT INTO sections_of_line (
    sol_id,
    start_op_uopid,
    end_op_uopid,
    country,
    length_meters,
    gauge,
    max_speed,
    electrified
)
SELECT
    'XB_CH00090_DE00RQB',
    'CH00090',
    'DE00RQB',
    'INT',
    0,
    NULL,
    NULL,
    NULL
WHERE EXISTS (SELECT 1 FROM operational_points WHERE uopid = 'CH00090')
  AND EXISTS (SELECT 1 FROM operational_points WHERE uopid = 'DE00RQB')
  AND NOT EXISTS (SELECT 1 FROM sections_of_line WHERE sol_id = 'XB_CH00090_DE00RQB');

INSERT INTO sections_of_line (
    sol_id,
    start_op_uopid,
    end_op_uopid,
    country,
    length_meters,
    gauge,
    max_speed,
    electrified
)
SELECT
    'XB_CH03424_DE0RQTG',
    'CH03424',
    'DE0RQTG',
    'INT',
    0,
    NULL,
    NULL,
    NULL
WHERE EXISTS (SELECT 1 FROM operational_points WHERE uopid = 'CH03424')
  AND EXISTS (SELECT 1 FROM operational_points WHERE uopid = 'DE0RQTG')
  AND NOT EXISTS (SELECT 1 FROM sections_of_line WHERE sol_id = 'XB_CH03424_DE0RQTG');

INSERT INTO sections_of_line (
    sol_id,
    start_op_uopid,
    end_op_uopid,
    country,
    length_meters,
    gauge,
    max_speed,
    electrified
)
SELECT
    'XB_CH18047_EU00026',
    'CH18047',
    'EU00026',
    'INT',
    0,
    NULL,
    NULL,
    NULL
WHERE EXISTS (SELECT 1 FROM operational_points WHERE uopid = 'CH18047')
  AND EXISTS (SELECT 1 FROM operational_points WHERE uopid = 'EU00026')
  AND NOT EXISTS (SELECT 1 FROM sections_of_line WHERE sol_id = 'XB_CH18047_EU00026');

INSERT INTO sections_of_line (
    sol_id,
    start_op_uopid,
    end_op_uopid,
    country,
    length_meters,
    gauge,
    max_speed,
    electrified
)
SELECT
    'XB_CH18048_EU00027',
    'CH18048',
    'EU00027',
    'INT',
    0,
    NULL,
    NULL,
    NULL
WHERE EXISTS (SELECT 1 FROM operational_points WHERE uopid = 'CH18048')
  AND EXISTS (SELECT 1 FROM operational_points WHERE uopid = 'EU00027')
  AND NOT EXISTS (SELECT 1 FROM sections_of_line WHERE sol_id = 'XB_CH18048_EU00027');
