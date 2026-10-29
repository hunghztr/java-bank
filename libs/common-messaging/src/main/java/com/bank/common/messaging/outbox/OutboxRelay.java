package com.bank.common.messaging.outbox;

import com.bank.common.events.Topics;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Định kỳ đẩy message chưa gửi trong outbox lên Kafka (at-least-once: có thể gửi trùng khi lỗi giữa chừng,
 * nên consumer phải idempotent). Production có thể thay bằng Debezium CDC đọc thẳng bảng outbox.
 * Chạy nhiều instance: thêm khoá (ShedLock) hoặc SELECT ... FOR UPDATE SKIP LOCKED để tránh gửi đồng thời.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:500}")
    @Transactional
    public void relay() {
        for (OutboxMessage m : repository.findTop100BySentAtIsNullOrderByCreatedAtAsc()) {
            ProducerRecord<String, String> record = new ProducerRecord<>(m.getTopic(), m.getMessageKey(), m.getPayload());
            record.headers().add(Topics.HEADER_MESSAGE_TYPE, m.getMessageType().getBytes(StandardCharsets.UTF_8));
            try {
                kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
                m.markSent();
            } catch (Exception e) {
                // Dừng lô hiện tại để giữ thứ tự; lần chạy sau thử lại
                log.warn("Gửi outbox {} thất bại, sẽ thử lại: {}", m.getId(), e.getMessage());
                return;
            }
        }
    }
}
