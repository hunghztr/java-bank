package com.bank.notification;

import com.bank.common.events.MessageCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @Bean
    MessageCodec messageCodec(ObjectMapper objectMapper) {
        return new MessageCodec(objectMapper);
    }
}
