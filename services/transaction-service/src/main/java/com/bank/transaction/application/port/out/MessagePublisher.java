package com.bank.transaction.application.port.out;

public interface MessagePublisher {

    /** Ghi message để gửi đi; phải gọi trong transaction nghiệp vụ (cài đặt bằng outbox). */
    void publish(String topic, String key, Object message);
}
