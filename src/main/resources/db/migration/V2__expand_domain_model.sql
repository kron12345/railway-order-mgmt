-- V2: Expand domain model with Customer, OrderPosition, ResourceNeed,
--     PurchasePosition, Business and update Orders table

-- ============================================================
-- Customers
-- ============================================================
CREATE TABLE customers (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_number  VARCHAR(50)  NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    contact_person   VARCHAR(255),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_customers_number ON customers (customer_number);
CREATE INDEX idx_customers_name ON customers (name);

CREATE TABLE customers_audit (
    id               UUID         NOT NULL,
    customer_number  VARCHAR(50),
    name             VARCHAR(255),
    contact_person   VARCHAR(255),
    status           VARCHAR(20),
    created_at       TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          BIGINT,
    revision_id      INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type    SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);

-- ============================================================
-- Expand Orders: add new columns, migrate status → process_status
-- ============================================================
ALTER TABLE orders ADD COLUMN name VARCHAR(255);
ALTER TABLE orders ADD COLUMN customer_id UUID REFERENCES customers(id);
ALTER TABLE orders ADD COLUMN comment VARCHAR(2000);
ALTER TABLE orders ADD COLUMN tags VARCHAR(1000);
ALTER TABLE orders ADD COLUMN valid_from DATE;
ALTER TABLE orders ADD COLUMN valid_to DATE;
ALTER TABLE orders ADD COLUMN timetable_year_label VARCHAR(50);
ALTER TABLE orders ADD COLUMN process_status VARCHAR(40) DEFAULT 'AUFTRAG';
ALTER TABLE orders ADD COLUMN internal_status VARCHAR(50);

-- Migrate existing status to process_status
UPDATE orders SET process_status = 'AUFTRAG' WHERE status = 'DRAFT';
UPDATE orders SET process_status = 'PLANUNG' WHERE status IN ('SUBMITTED', 'CONFIRMED');
UPDATE orders SET process_status = 'PRODUKTION' WHERE status = 'IN_TRANSIT';
UPDATE orders SET process_status = 'ABRECHNUNG_NACHBEREITUNG' WHERE status = 'DELIVERED';
UPDATE orders SET name = order_number WHERE name IS NULL;

ALTER TABLE orders ALTER COLUMN name SET NOT NULL;
ALTER TABLE orders DROP COLUMN status;
ALTER TABLE orders DROP COLUMN description;

CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_process_status ON orders (process_status);

-- Update orders audit table
ALTER TABLE orders_audit ADD COLUMN name VARCHAR(255);
ALTER TABLE orders_audit ADD COLUMN customer_id UUID;
ALTER TABLE orders_audit ADD COLUMN comment VARCHAR(2000);
ALTER TABLE orders_audit ADD COLUMN tags VARCHAR(1000);
ALTER TABLE orders_audit ADD COLUMN valid_from DATE;
ALTER TABLE orders_audit ADD COLUMN valid_to DATE;
ALTER TABLE orders_audit ADD COLUMN timetable_year_label VARCHAR(50);
ALTER TABLE orders_audit ADD COLUMN process_status VARCHAR(40);
ALTER TABLE orders_audit ADD COLUMN internal_status VARCHAR(50);
ALTER TABLE orders_audit DROP COLUMN IF EXISTS status;
ALTER TABLE orders_audit DROP COLUMN IF EXISTS description;

-- ============================================================
-- Order Positions
-- ============================================================
CREATE TABLE order_positions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    name              VARCHAR(255) NOT NULL,
    type              VARCHAR(20)  NOT NULL,
    tags              VARCHAR(1000),
    start             TIMESTAMP,
    "end"             TIMESTAMP,
    service_type      VARCHAR(100),
    from_location     VARCHAR(255),
    to_location       VARCHAR(255),
    validity          JSONB,
    internal_status   VARCHAR(30)  DEFAULT 'IN_BEARBEITUNG',
    variant_of_id     UUID         REFERENCES order_positions(id),
    variant_type      VARCHAR(30),
    merge_target_id   UUID         REFERENCES order_positions(id),
    merge_status      VARCHAR(20),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_positions_order ON order_positions (order_id);
CREATE INDEX idx_positions_type ON order_positions (type);
CREATE INDEX idx_positions_status ON order_positions (internal_status);

CREATE TABLE order_positions_audit (
    id                UUID         NOT NULL,
    order_id          UUID,
    name              VARCHAR(255),
    type              VARCHAR(20),
    tags              VARCHAR(1000),
    start             TIMESTAMP,
    "end"             TIMESTAMP,
    service_type      VARCHAR(100),
    from_location     VARCHAR(255),
    to_location       VARCHAR(255),
    validity          JSONB,
    internal_status   VARCHAR(30),
    variant_of_id     UUID,
    variant_type      VARCHAR(30),
    merge_target_id   UUID,
    merge_status      VARCHAR(20),
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    version           BIGINT,
    revision_id       INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type     SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);

-- ============================================================
-- Resource Needs
-- ============================================================
CREATE TABLE resource_needs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_position_id    UUID        NOT NULL REFERENCES order_positions(id) ON DELETE CASCADE,
    resource_type        VARCHAR(20) NOT NULL,
    coverage_type        VARCHAR(20) NOT NULL,
    status               VARCHAR(30),
    linked_fahrplan_id   UUID
);

CREATE INDEX idx_resource_needs_position ON resource_needs (order_position_id);

CREATE TABLE resource_needs_audit (
    id                   UUID        NOT NULL,
    order_position_id    UUID,
    resource_type        VARCHAR(20),
    coverage_type        VARCHAR(20),
    status               VARCHAR(30),
    linked_fahrplan_id   UUID,
    revision_id          INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type        SMALLINT    NOT NULL,
    PRIMARY KEY (id, revision_id)
);

-- ============================================================
-- Purchase Positions
-- ============================================================
CREATE TABLE purchase_positions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    position_number      VARCHAR(50) NOT NULL UNIQUE,
    order_position_id    UUID        NOT NULL REFERENCES order_positions(id) ON DELETE CASCADE,
    resource_need_id     UUID        NOT NULL REFERENCES resource_needs(id),
    validity             JSONB,
    debicode             VARCHAR(50),
    purchase_status      VARCHAR(20) NOT NULL DEFAULT 'OFFEN',
    ordered_at           TIMESTAMPTZ,
    status_timestamp     TIMESTAMPTZ
);

CREATE INDEX idx_purchase_position ON purchase_positions (order_position_id);
CREATE INDEX idx_purchase_resource ON purchase_positions (resource_need_id);

CREATE TABLE purchase_positions_audit (
    id                   UUID        NOT NULL,
    position_number      VARCHAR(50),
    order_position_id    UUID,
    resource_need_id     UUID,
    validity             JSONB,
    debicode             VARCHAR(50),
    purchase_status      VARCHAR(20),
    ordered_at           TIMESTAMPTZ,
    status_timestamp     TIMESTAMPTZ,
    revision_id          INTEGER     NOT NULL REFERENCES revinfo(rev),
    revision_type        SMALLINT    NOT NULL,
    PRIMARY KEY (id, revision_id)
);

-- ============================================================
-- Businesses (Geschaefte)
-- ============================================================
CREATE TABLE businesses (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title             VARCHAR(255) NOT NULL,
    description       VARCHAR(2000) NOT NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'IN_BEARBEITUNG',
    assignment_type   VARCHAR(30),
    assignment_name   VARCHAR(255),
    team              VARCHAR(255),
    valid_from        DATE,
    valid_to          DATE,
    due_date          DATE,
    documents         JSONB,
    tags              VARCHAR(1000),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE businesses_audit (
    id                UUID         NOT NULL,
    title             VARCHAR(255),
    description       VARCHAR(2000),
    status            VARCHAR(30),
    assignment_type   VARCHAR(30),
    assignment_name   VARCHAR(255),
    team              VARCHAR(255),
    valid_from        DATE,
    valid_to          DATE,
    due_date          DATE,
    documents         JSONB,
    tags              VARCHAR(1000),
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    version           BIGINT,
    revision_id       INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type     SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);

-- ============================================================
-- Business <-> OrderPosition (m:n)
-- ============================================================
CREATE TABLE business_order_positions (
    business_id       UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    order_position_id UUID NOT NULL REFERENCES order_positions(id) ON DELETE CASCADE,
    PRIMARY KEY (business_id, order_position_id)
);
