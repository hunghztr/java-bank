package com.bank.transaction.adapter.out.persistence;

import com.bank.transaction.domain.model.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
class TransferJpaEntity {

    @Id
    UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    String idempotencyKey;

    @Column(name = "from_account", nullable = false)
    String fromAccount;

    @Column(name = "to_account", nullable = false)
    String toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TransferStatus status;

    @Column(name = "failure_reason")
    String failureReason;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    // Khoá lạc quan: hai reply cùng lúc cho một giao dịch thì một bên sẽ lỗi và được Kafka thử lại
    @Version
    long version;
}
