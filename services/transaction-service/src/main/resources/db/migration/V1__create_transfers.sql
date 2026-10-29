CREATE TABLE transfers (
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(100)   NOT NULL UNIQUE,
    from_account    VARCHAR(16)    NOT NULL,
    to_account      VARCHAR(16)    NOT NULL,
    amount          NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    status          VARCHAR(20)    NOT NULL,
    failure_reason  VARCHAR(500),
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_transfers_from_account ON transfers (from_account, created_at DESC);
