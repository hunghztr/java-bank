package com.bank.account.messaging;

import com.bank.account.application.AccountCommandHandler;
import com.bank.common.events.MessageCodec;
import com.bank.common.events.Topics;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AccountCommandListener {

    private final MessageCodec codec;
    private final AccountCommandHandler handler;

    public AccountCommandListener(MessageCodec codec, AccountCommandHandler handler) {
        this.codec = codec;
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.ACCOUNT_COMMANDS)
    public void onCommand(ConsumerRecord<String, String> record) {
        Header type = record.headers().lastHeader(Topics.HEADER_MESSAGE_TYPE);
        if (type == null) {
            throw new IllegalArgumentException("Message thiếu header " + Topics.HEADER_MESSAGE_TYPE);
        }
        handler.handle(codec.decode(new String(type.value(), StandardCharsets.UTF_8), record.value()));
    }
}
