-- V5: Predefined tags for orders, positions, and general use

CREATE TABLE predefined_tags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)  NOT NULL,
    category    VARCHAR(30)   NOT NULL DEFAULT 'GENERAL',
    color       VARCHAR(20),
    sort_order  INTEGER       NOT NULL DEFAULT 0,
    active      BOOLEAN       NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_tags_name_category ON predefined_tags (name, category);
CREATE INDEX idx_tags_category ON predefined_tags (category);

-- Tag master data is imported separately via CSV in Settings > Tags.
