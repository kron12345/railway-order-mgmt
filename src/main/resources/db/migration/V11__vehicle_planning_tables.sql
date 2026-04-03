-- Vehicle Planning tables for rotation management
-- No audit tables: planning data is not audited via Envers

CREATE TABLE vp_rotation_sets (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    name              VARCHAR(100) NOT NULL,
    description       VARCHAR(500),
    timetable_year_id UUID         NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_vp_rotation_sets PRIMARY KEY (id),
    CONSTRAINT fk_vp_rotation_sets_tty FOREIGN KEY (timetable_year_id)
        REFERENCES pm_timetable_years (id)
);

CREATE TABLE vp_vehicles (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    rotation_set_id UUID         NOT NULL,
    label           VARCHAR(100) NOT NULL,
    vehicle_type    VARCHAR(30)  NOT NULL,
    vehicle_class   VARCHAR(50),
    sequence        INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_vp_vehicles PRIMARY KEY (id),
    CONSTRAINT fk_vp_vehicles_rotation_set FOREIGN KEY (rotation_set_id)
        REFERENCES vp_rotation_sets (id) ON DELETE CASCADE
);

CREATE TABLE vp_rotation_entries (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    vehicle_id          UUID        NOT NULL,
    reference_train_id  UUID        NOT NULL,
    day_of_week         INTEGER     NOT NULL,
    sequence_in_day     INTEGER     NOT NULL DEFAULT 0,
    coupling_type       VARCHAR(20) NOT NULL DEFAULT 'FULL',
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now(),
    version             BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_vp_rotation_entries PRIMARY KEY (id),
    CONSTRAINT fk_vp_rotation_entries_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vp_vehicles (id) ON DELETE CASCADE,
    CONSTRAINT fk_vp_rotation_entries_train FOREIGN KEY (reference_train_id)
        REFERENCES pm_reference_trains (id),
    CONSTRAINT chk_vp_rotation_entries_dow CHECK (day_of_week BETWEEN 1 AND 7)
);

CREATE TABLE vp_vehicle_operations (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    rotation_entry_id     UUID         NOT NULL,
    location_name         VARCHAR(255),
    activity_code         VARCHAR(50),
    associated_train_otn  VARCHAR(20),
    composition_section   VARCHAR(50),
    comment               VARCHAR(500),
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT now(),
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_vp_vehicle_operations PRIMARY KEY (id),
    CONSTRAINT fk_vp_vehicle_operations_entry FOREIGN KEY (rotation_entry_id)
        REFERENCES vp_rotation_entries (id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_vp_rotation_sets_tty ON vp_rotation_sets (timetable_year_id);
CREATE INDEX idx_vp_vehicles_rotation_set ON vp_vehicles (rotation_set_id);
CREATE INDEX idx_vp_rotation_entries_vehicle ON vp_rotation_entries (vehicle_id);
CREATE INDEX idx_vp_rotation_entries_train ON vp_rotation_entries (reference_train_id);
CREATE INDEX idx_vp_rotation_entries_dow ON vp_rotation_entries (day_of_week);
CREATE INDEX idx_vp_vehicle_operations_entry ON vp_vehicle_operations (rotation_entry_id);
