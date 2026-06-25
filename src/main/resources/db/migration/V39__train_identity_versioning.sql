-- V39: Train-identity hierarchy + versioning (concept 1, slice K1-S1). Adds the per-expression
-- version trail and the OTN history so all changes to a train stay traceable. The hierarchy columns
-- variant_of_id / variant_type already exist (V2) and are now activated by the entity model
-- (variant_type becomes the PositionVariantType enum) — no DDL needed there.

CREATE TABLE order_position_versions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_position_id UUID         NOT NULL REFERENCES order_positions(id) ON DELETE CASCADE,
    version_number    INTEGER      NOT NULL,
    source            VARCHAR(20)  NOT NULL,
    valid_from        DATE,
    valid_to          DATE,
    change_summary    VARCHAR(500),
    payload           JSONB,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_opv_position_version UNIQUE (order_position_id, version_number)
);
CREATE INDEX idx_opv_position ON order_position_versions (order_position_id);

CREATE TABLE order_position_versions_audit (
    id                UUID         NOT NULL,
    order_position_id UUID,
    version_number    INTEGER,
    source            VARCHAR(20),
    valid_from        DATE,
    valid_to          DATE,
    change_summary    VARCHAR(500),
    payload           JSONB,
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT,
    revision_id       INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type     SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);

CREATE TABLE position_otn_history (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_position_id UUID         NOT NULL REFERENCES order_positions(id) ON DELETE CASCADE,
    otn               VARCHAR(20)  NOT NULL,
    valid_from        DATE,
    valid_to          DATE,
    source            VARCHAR(20),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        VARCHAR(100)
);
CREATE INDEX idx_otnhist_position ON position_otn_history (order_position_id);
CREATE INDEX idx_otnhist_otn ON position_otn_history (otn);

CREATE TABLE position_otn_history_audit (
    id                UUID         NOT NULL,
    order_position_id UUID,
    otn               VARCHAR(20),
    valid_from        DATE,
    valid_to          DATE,
    source            VARCHAR(20),
    created_at        TIMESTAMPTZ,
    created_by        VARCHAR(100),
    revision_id       INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type     SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);
