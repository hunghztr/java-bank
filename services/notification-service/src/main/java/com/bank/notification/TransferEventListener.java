package com.bank.notification;

import com.bank.common.events.MessageCodec;
import com.bank.common.events.Topics;
import com.bank.common.events.TransferEvents.TransferCompleted;
import com.bank.common.events.TransferEvents.TransferFailed;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Nghe sự kiện kết thúc giao dịch để gửi thông báo. Thay log bằng email/SMS/push thực tế. */
@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);

    private final MessageCodec codec;

    public TransferEventListener(MessageCodec codec) {
        this.codec = codec;
    }

    @KafkaListener(topics = Topics.TRANSFER_EVENTS)
    public void onEvent(ConsumerRecord<String, String> record) {
        Header type = record.headers().lastHeader(Topics.HEADER_MESSAGE_TYPE);
        if (type == null) {
            log.warn("Bỏ qua message thiếu header loại tại offset {}", record.offset());
            return;
        }
        switch (codec.decode(new String(type.value(), StandardCharsets.UTF_8), record.value())) {
            case TransferCompleted e -> log.info("[THÔNG BÁO] TK {} đã chuyển {} tới TK {} thành công",
                    e.fromAccount(), e.amount(), e.toAccount());
            case TransferFailed e -> log.info("[THÔNG BÁO] Giao dịch {} từ TK {} thất bại: {}",
                    e.transferId(), e.fromAccount(), e.reason());
            default -> log.debug("Bỏ qua sự kiện {}", record.key());
        }
    }
}
