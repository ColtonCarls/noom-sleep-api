CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Seed test users so the API can be exercised immediately.
INSERT INTO users (name) VALUES ('Alice'), ('Bob'), ('Charlie');
