package com.bank.common.messaging.outbox;

import com.bank.common.events.MessageCodec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxWriter {

    private final OutboxRepository repository;
    private final MessageCodec codec;

    public OutboxWriter(OutboxRepository repository, MessageCodec codec) {
        this.repository = repository;
        this.codec = codec;
    }

    /** Bắt buộc gọi trong transaction nghiệp vụ đang mở (MANDATORY) để ghi nguyên tử cùng dữ liệu. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String topic, String key, Object message) {
        repository.save(new OutboxMessage(topic, key, MessageCodec.typeOf(message), codec.encode(message)));
    }
}
