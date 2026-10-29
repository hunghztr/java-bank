package com.bank.transaction.adapter.in.messaging;

import com.bank.common.events.MessageCodec;
import com.bank.common.events.Topics;
import com.bank.transaction.application.saga.TransferSaga;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AccountReplyListener {

    private final MessageCodec codec;
    private final TransferSaga saga;

    public AccountReplyListener(MessageCodec codec, TransferSaga saga) {
        this.codec = codec;
        this.saga = saga;
    }

    @KafkaListener(topics = Topics.ACCOUNT_REPLIES)
    public void onReply(ConsumerRecord<String, String> record) {
        Header type = record.headers().lastHeader(Topics.HEADER_MESSAGE_TYPE);
        if (type == null) {
            throw new IllegalArgumentException("Message thiếu header " + Topics.HEADER_MESSAGE_TYPE);
        }
        saga.on(codec.decode(new String(type.value(), StandardCharsets.UTF_8), record.value()));
    }
}
