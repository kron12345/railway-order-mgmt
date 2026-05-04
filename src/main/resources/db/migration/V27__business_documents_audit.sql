-- Envers audit table for business_documents
-- Mirrors columns of business_documents plus revision metadata.

CREATE TABLE business_documents_audit (
    id              UUID         NOT NULL,
    business_id     UUID,
    filename        VARCHAR(500),
    content_type    VARCHAR(200),
    data            BYTEA,
    created_at      TIMESTAMPTZ,
    revision_id     INTEGER      NOT NULL REFERENCES revinfo(rev),
    revision_type   SMALLINT     NOT NULL,
    PRIMARY KEY (id, revision_id)
);
