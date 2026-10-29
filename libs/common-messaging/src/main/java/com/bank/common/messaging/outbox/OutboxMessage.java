package com.bank.common.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Bản ghi outbox: được ghi CÙNG transaction với dữ liệu nghiệp vụ, sau đó OutboxRelay mới đẩy lên Kafka.
 * Nhờ vậy không có chuyện DB đã commit mà message bị mất (hoặc ngược lại).
 */
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxMessage() {
    }

    public OutboxMessage(String topic, String messageKey, String messageType, String payload) {
        this.id = UUID.randomUUID();
        this.topic = topic;
        this.messageKey = messageKey;
        this.messageType = messageType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markSent() {
        this.sentAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getPayload() {
        return payload;
    }
}
