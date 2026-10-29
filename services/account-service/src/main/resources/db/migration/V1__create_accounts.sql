CREATE TABLE accounts (
    id             UUID PRIMARY KEY,
    account_number VARCHAR(16)    NOT NULL UNIQUE,
    owner_name     VARCHAR(200)   NOT NULL,
    balance        NUMERIC(19, 2) NOT NULL CHECK (balance >= 0),
    currency       CHAR(3)        NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE processed_messages (
    message_id   VARCHAR(200) PRIMARY KEY,
    processed_at TIMESTAMPTZ  NOT NULL
);
