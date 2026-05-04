-- Rolling stock master data (Fahrzeug-Stammdaten)
CREATE TABLE rs_rolling_stock (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evn                 VARCHAR(12),
    designation         VARCHAR(50) NOT NULL,
    keeper_code         VARCHAR(10),
    owner_country_code  VARCHAR(2),
    vehicle_category    VARCHAR(20) NOT NULL,
    uic_letter_code     VARCHAR(20),
    traction_type       VARCHAR(4),
    number_of_axles     INTEGER,
    length_over_buffers INTEGER,
    weight_empty        INTEGER,
    max_speed           INTEGER,
    power_output        INTEGER,
    traction_system     VARCHAR(30),
    axle_arrangement    VARCHAR(30),
    seats1st_class      INTEGER,
    seats2nd_class      INTEGER,
    max_payload         INTEGER,
    coupling_type       VARCHAR(30),
    brake_type          VARCHAR(30),
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT NOT NULL DEFAULT 0
);

-- Envers audit table
CREATE TABLE rs_rolling_stock_audit (
    id                  UUID NOT NULL,
    revision_id         INTEGER NOT NULL REFERENCES revinfo(rev),
    revision_type       SMALLINT,
    evn                 VARCHAR(12),
    designation         VARCHAR(50),
    keeper_code         VARCHAR(10),
    owner_country_code  VARCHAR(2),
    vehicle_category    VARCHAR(20),
    uic_letter_code     VARCHAR(20),
    traction_type       VARCHAR(4),
    number_of_axles     INTEGER,
    length_over_buffers INTEGER,
    weight_empty        INTEGER,
    max_speed           INTEGER,
    power_output        INTEGER,
    traction_system     VARCHAR(30),
    axle_arrangement    VARCHAR(30),
    seats1st_class      INTEGER,
    seats2nd_class      INTEGER,
    max_payload         INTEGER,
    coupling_type       VARCHAR(30),
    brake_type          VARCHAR(30),
    active              BOOLEAN,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    PRIMARY KEY (id, revision_id)
);

-- === Example rolling stock ===

-- 1) SBB RABe 502.001 — Twindexx IC (8-car double-deck EMU)
INSERT INTO rs_rolling_stock (designation, evn, keeper_code, owner_country_code, vehicle_category,
    traction_type, number_of_axles, length_over_buffers, weight_empty, max_speed, power_output,
    traction_system, axle_arrangement, seats1st_class, seats2nd_class, coupling_type, brake_type)
VALUES ('RABe 502 IC', '948505020019', 'SBB', '85', 'EMU',
    '13', 16, 200600, 453000, 200, 7500,
    '15kV 16.7Hz AC', 'Bo''Bo''+2''2''+Bo''Bo''', 176, 430, 'Scharfenberg', 'ep');

-- 2) SBB RABe 502.201 — Twindexx IR 200 (8-car)
INSERT INTO rs_rolling_stock (designation, evn, keeper_code, owner_country_code, vehicle_category,
    traction_type, number_of_axles, length_over_buffers, weight_empty, max_speed, power_output,
    traction_system, seats1st_class, seats2nd_class, coupling_type, brake_type)
VALUES ('RABe 502 IR200', '948505022015', 'SBB', '85', 'EMU',
    '13', 16, 200600, 453000, 200, 7500,
    '15kV 16.7Hz AC', 181, 501, 'Scharfenberg', 'ep');

-- 3) SBB RABe 502.401 — Twindexx IR 100 (4-car)
INSERT INTO rs_rolling_stock (designation, evn, keeper_code, owner_country_code, vehicle_category,
    traction_type, number_of_axles, length_over_buffers, weight_empty, max_speed, power_output,
    traction_system, seats1st_class, seats2nd_class, coupling_type, brake_type)
VALUES ('RABe 502 IR100', '948505024011', 'SBB', '85', 'EMU',
    '13', 8, 101000, 270000, 200, 3750,
    '15kV 16.7Hz AC', 47, 283, 'Scharfenberg', 'ep');

-- 4) SBB Re 460.021 — Lok 2000 (electric locomotive)
INSERT INTO rs_rolling_stock (designation, evn, keeper_code, owner_country_code, vehicle_category,
    traction_type, number_of_axles, length_over_buffers, weight_empty, max_speed, power_output,
    traction_system, axle_arrangement, coupling_type, brake_type)
VALUES ('Re 460', '918544600219', 'SBB', '85', 'LOCOMOTIVE',
    '11', 4, 18500, 84000, 200, 6100,
    '15kV 16.7Hz AC', 'Bo''Bo''', 'Screw', 'KE-GPR');

-- 5) SBB EW IV A — 1st class coach
INSERT INTO rs_rolling_stock (designation, evn, keeper_code, owner_country_code, vehicle_category,
    number_of_axles, length_over_buffers, weight_empty, max_speed,
    seats1st_class, coupling_type, brake_type)
VALUES ('EW IV A', '508510730526', 'SBB', '85', 'COACH',
    4, 26400, 42000, 160,
    60, 'Screw', 'KE-GP-A');

-- 6) SBB EW IV B — 2nd class coach
INSERT INTO rs_rolling_stock (designation, evn, keeper_code, owner_country_code, vehicle_category,
    number_of_axles, length_over_buffers, weight_empty, max_speed,
    seats2nd_class, coupling_type, brake_type)
VALUES ('EW IV B', '508521730489', 'SBB', '85', 'COACH',
    4, 26400, 40000, 160,
    78, 'Screw', 'KE-GP-A');

-- 7) Habbins 354 — Covered sliding-wall freight wagon
INSERT INTO rs_rolling_stock (designation, keeper_code, owner_country_code, vehicle_category,
    uic_letter_code, number_of_axles, length_over_buffers, weight_empty, max_speed,
    max_payload, coupling_type, brake_type)
VALUES ('Habbins 354', 'SBBCARGO', '85', 'FREIGHT_WAGON',
    'Habbins', 4, 22000, 26500, 120,
    37500, 'Screw', 'KE-GP-A');

-- 8) Sgns 681 — Container flat wagon
INSERT INTO rs_rolling_stock (designation, keeper_code, owner_country_code, vehicle_category,
    uic_letter_code, number_of_axles, length_over_buffers, weight_empty, max_speed,
    max_payload, coupling_type, brake_type)
VALUES ('Sgns 681', 'SBBCARGO', '85', 'FREIGHT_WAGON',
    'Sgns', 4, 18400, 18800, 120,
    45000, 'Screw', 'KE-GP-A');
