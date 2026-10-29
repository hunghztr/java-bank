package com.bank.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mã hoá / giải mã message JSON. Tên loại message (simple class name) đi kèm trong header Kafka,
 * consumer dựa vào đó để biết deserialize ra class nào.
 */
public class MessageCodec {

    private static final Map<String, Class<?>> TYPES = Stream.of(
            AccountCommands.DebitAccount.class,
            AccountCommands.CreditAccount.class,
            AccountCommands.RefundAccount.class,
            AccountReplies.AccountDebited.class,
            AccountReplies.AccountDebitFailed.class,
            AccountReplies.AccountCredited.class,
            AccountReplies.AccountCreditFailed.class,
            AccountReplies.AccountRefunded.class,
            TransferEvents.TransferCompleted.class,
            TransferEvents.TransferFailed.class
    ).collect(Collectors.toUnmodifiableMap(Class::getSimpleName, Function.identity()));

    private final ObjectMapper objectMapper;

    public MessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static String typeOf(Object message) {
        return message.getClass().getSimpleName();
    }

    public String encode(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Không serialize được message " + typeOf(message), e);
        }
    }

    public Object decode(String type, String payload) {
        Class<?> clazz = TYPES.get(type);
        if (clazz == null) {
            throw new IllegalArgumentException("Loại message không xác định: " + type);
        }
        try {
            return objectMapper.readValue(payload, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Không đọc được message " + type, e);
        }
    }
}
