CREATE TABLE outbox_messages (
    id           UUID PRIMARY KEY,
    topic        VARCHAR(200) NOT NULL,
    message_key  VARCHAR(200) NOT NULL,
    message_type VARCHAR(200) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    sent_at      TIMESTAMPTZ
);

-- Relay chỉ quét bản ghi chưa gửi
CREATE INDEX idx_outbox_unsent ON outbox_messages (created_at) WHERE sent_at IS NULL;
