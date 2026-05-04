-- ==========
-- Geschäfte: Dokumente als BYTEA in separater Tabelle
-- ==========

CREATE TABLE business_documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id       UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    filename          VARCHAR(500) NOT NULL,
    content_type      VARCHAR(200) NOT NULL,
    data              BYTEA NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bd_business ON business_documents(business_id);
