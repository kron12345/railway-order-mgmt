-- V1: Initial schema for Railway Order Management

-- Orders
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(50)  NOT NULL UNIQUE,
    status          VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    description     VARCHAR(2000),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_order_number ON orders (order_number);

-- Hibernate Envers revision tracking
CREATE TABLE revinfo (
    rev         SERIAL PRIMARY KEY,
    revtstmp    BIGINT NOT NULL
);

CREATE TABLE orders_audit (
    id              UUID         NOT NULL,
    order_number    VARCHAR(50),
    status          VARCHAR(30),
    description     VARCHAR(2000),
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT,
    revision_id     INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type   SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);
