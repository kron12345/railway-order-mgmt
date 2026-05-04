-- Business <-> PurchasePosition (m:n) join + Envers audit table.

CREATE TABLE business_purchase_positions (
    business_id          UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    purchase_position_id UUID NOT NULL REFERENCES purchase_positions(id) ON DELETE CASCADE,
    PRIMARY KEY (business_id, purchase_position_id)
);

CREATE TABLE business_purchase_positions_audit (
    business_id          UUID         NOT NULL,
    purchase_position_id UUID         NOT NULL,
    revision_id          INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type        SMALLINT     NOT NULL,
    PRIMARY KEY (business_id, purchase_position_id, revision_id)
);
