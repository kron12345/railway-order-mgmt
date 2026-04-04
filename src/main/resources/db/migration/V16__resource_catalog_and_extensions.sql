-- Resource Catalog (Fahrzeugtypen + Personalqualifikationen)
CREATE TABLE resource_catalog_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category    VARCHAR(20) NOT NULL,
    code        VARCHAR(20) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT true,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT NOT NULL DEFAULT 0,
    UNIQUE(category, code)
);

CREATE TABLE resource_catalog_items_audit (
    id            UUID NOT NULL,
    revision_id   INTEGER NOT NULL REFERENCES revinfo(rev),
    revision_type SMALLINT NOT NULL,
    category      VARCHAR(20),
    code          VARCHAR(20),
    name          VARCHAR(255),
    description   TEXT,
    active        BOOLEAN,
    sort_order    INT,
    PRIMARY KEY (id, revision_id)
);

-- Extend resource_needs
ALTER TABLE resource_needs ADD COLUMN description VARCHAR(255);
ALTER TABLE resource_needs ADD COLUMN quantity INT DEFAULT 1;
ALTER TABLE resource_needs ADD COLUMN catalog_item_id UUID REFERENCES resource_catalog_items(id);
ALTER TABLE resource_needs ADD COLUMN valid_from DATE;
ALTER TABLE resource_needs ADD COLUMN valid_to DATE;
ALTER TABLE resource_needs ADD COLUMN priority VARCHAR(10) DEFAULT 'MEDIUM';
ALTER TABLE resource_needs ADD COLUMN origin VARCHAR(20) DEFAULT 'MANUAL';

ALTER TABLE resource_needs_audit ADD COLUMN description VARCHAR(255);
ALTER TABLE resource_needs_audit ADD COLUMN quantity INT;
ALTER TABLE resource_needs_audit ADD COLUMN catalog_item_id UUID;
ALTER TABLE resource_needs_audit ADD COLUMN valid_from DATE;
ALTER TABLE resource_needs_audit ADD COLUMN valid_to DATE;
ALTER TABLE resource_needs_audit ADD COLUMN priority VARCHAR(10);
ALTER TABLE resource_needs_audit ADD COLUMN origin VARCHAR(20);

-- Extend purchase_positions
ALTER TABLE purchase_positions ADD COLUMN description VARCHAR(255);
ALTER TABLE purchase_positions ADD COLUMN pm_path_request_id UUID;
ALTER TABLE purchase_positions ADD COLUMN pm_path_id UUID;
ALTER TABLE purchase_positions ADD COLUMN pm_process_state VARCHAR(30);
ALTER TABLE purchase_positions ADD COLUMN pm_ttr_phase VARCHAR(30);
ALTER TABLE purchase_positions ADD COLUMN pm_last_synced TIMESTAMPTZ;

ALTER TABLE purchase_positions_audit ADD COLUMN description VARCHAR(255);
ALTER TABLE purchase_positions_audit ADD COLUMN pm_path_request_id UUID;
ALTER TABLE purchase_positions_audit ADD COLUMN pm_path_id UUID;
ALTER TABLE purchase_positions_audit ADD COLUMN pm_process_state VARCHAR(30);
ALTER TABLE purchase_positions_audit ADD COLUMN pm_ttr_phase VARCHAR(30);
ALTER TABLE purchase_positions_audit ADD COLUMN pm_last_synced TIMESTAMPTZ;

-- Seed catalog data
INSERT INTO resource_catalog_items (id, category, code, name, sort_order) VALUES
    (gen_random_uuid(), 'VEHICLE_TYPE', 'RABE502', 'RABe 502 FLIRT', 1),
    (gen_random_uuid(), 'VEHICLE_TYPE', 'RABE511', 'RABe 511', 2),
    (gen_random_uuid(), 'VEHICLE_TYPE', 'RE460', 'Re 460', 3),
    (gen_random_uuid(), 'VEHICLE_TYPE', 'RE420', 'Re 420', 4),
    (gen_random_uuid(), 'VEHICLE_TYPE', 'IC2000', 'IC 2000 Wagen', 5),
    (gen_random_uuid(), 'VEHICLE_TYPE', 'EW4', 'Einheitswagen IV', 6),
    (gen_random_uuid(), 'PERSONNEL_QUAL', 'LF', 'Lokführer/in', 1),
    (gen_random_uuid(), 'PERSONNEL_QUAL', 'ZF', 'Zugführer/in', 2),
    (gen_random_uuid(), 'PERSONNEL_QUAL', 'RG', 'Rangierleiter/in', 3),
    (gen_random_uuid(), 'PERSONNEL_QUAL', 'KUCO', 'Kundenbegleiter/in', 4);
