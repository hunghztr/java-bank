package com.bank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Quét thêm com.bank.common (outbox, xử lý lỗi dùng chung)
@SpringBootApplication(scanBasePackages = "com.bank")
@EntityScan(basePackages = {"com.bank.account", "com.bank.common"})
@EnableJpaRepositories(basePackages = {"com.bank.account", "com.bank.common"})
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
