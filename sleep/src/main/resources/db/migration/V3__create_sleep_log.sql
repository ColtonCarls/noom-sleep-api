CREATE TYPE morning_feeling AS ENUM ('BAD', 'OK', 'GOOD');

CREATE TABLE sleep_log (
    id                   BIGSERIAL    PRIMARY KEY,
    user_id              BIGINT       NOT NULL REFERENCES users(id),
    bed_time             TIMESTAMPTZ  NOT NULL,
    wake_time            TIMESTAMPTZ  NOT NULL,
    feeling              morning_feeling NOT NULL,
    sleep_date           DATE         GENERATED ALWAYS AS ((wake_time AT TIME ZONE 'UTC')::date) STORED NOT NULL,
    total_minutes_in_bed INTEGER      GENERATED ALWAYS AS (EXTRACT(EPOCH FROM (wake_time - bed_time)) / 60) STORED NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_interval CHECK (wake_time > bed_time),
    CONSTRAINT uq_user_sleep_date UNIQUE (user_id, sleep_date)
);

CREATE INDEX idx_sleep_log_user_date ON sleep_log (user_id, sleep_date DESC);
