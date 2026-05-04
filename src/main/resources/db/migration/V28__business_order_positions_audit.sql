-- Envers audit table for the business <-> order_position join table.

CREATE TABLE business_order_positions_audit (
    business_id       UUID         NOT NULL,
    order_position_id UUID         NOT NULL,
    revision_id       INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type     SMALLINT     NOT NULL,
    PRIMARY KEY (business_id, order_position_id, revision_id)
);
