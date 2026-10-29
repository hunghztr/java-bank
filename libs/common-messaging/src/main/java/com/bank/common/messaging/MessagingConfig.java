package com.bank.common.messaging;

import com.bank.common.events.MessageCodec;
import com.bank.common.events.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableScheduling
public class MessagingConfig {

    @Bean
    public MessageCodec messageCodec(ObjectMapper objectMapper) {
        return new MessageCodec(objectMapper);
    }

    /** Consumer lỗi: thử lại 3 lần cách 1s, vẫn lỗi thì chuyển sang topic <tên>.DLT để xử lý tay */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public NewTopic accountCommandsTopic() {
        return TopicBuilder.name(Topics.ACCOUNT_COMMANDS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic accountRepliesTopic() {
        return TopicBuilder.name(Topics.ACCOUNT_REPLIES).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transferEventsTopic() {
        return TopicBuilder.name(Topics.TRANSFER_EVENTS).partitions(3).replicas(1).build();
    }
}
