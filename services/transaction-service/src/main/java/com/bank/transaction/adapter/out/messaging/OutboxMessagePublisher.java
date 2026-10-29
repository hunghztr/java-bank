package com.bank.transaction.adapter.out.messaging;

import com.bank.common.messaging.outbox.OutboxWriter;
import com.bank.transaction.application.port.out.MessagePublisher;
import org.springframework.stereotype.Component;

@Component
class OutboxMessagePublisher implements MessagePublisher {

    private final OutboxWriter outbox;

    OutboxMessagePublisher(OutboxWriter outbox) {
        this.outbox = outbox;
    }

    @Override
    public void publish(String topic, String key, Object message) {
        outbox.write(topic, key, message);
    }
}
