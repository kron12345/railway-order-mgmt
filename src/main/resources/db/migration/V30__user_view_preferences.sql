-- Per-user persistence for view preferences (grid layouts, splitter positions, filters, ...).
-- Generic by design: payload is JSON keyed by view_key so any future view can persist state here.

CREATE TABLE user_view_preferences (
    id          UUID         PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    view_key    VARCHAR(255) NOT NULL,
    payload     JSONB        NOT NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_view UNIQUE (user_id, view_key)
);

CREATE INDEX idx_user_view_preferences_user ON user_view_preferences (user_id);
