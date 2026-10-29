package com.bank.transaction.domain.model;

import com.bank.transaction.domain.exception.TransferValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Aggregate giao dịch chuyển tiền: Java thuần, không phụ thuộc Spring/JPA, tự bảo vệ các bước chuyển trạng thái. */
public class Transfer {

    private final UUID id;
    private final String idempotencyKey;
    private final String fromAccount;
    private final String toAccount;
    private final BigDecimal amount;
    private final Instant createdAt;
    private TransferStatus status;
    private String failureReason;
    private Instant updatedAt;

    public Transfer(UUID id, String idempotencyKey, String fromAccount, String toAccount, BigDecimal amount,
                    TransferStatus status, String failureReason, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Transfer create(String idempotencyKey, String fromAccount, String toAccount, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new TransferValidationException("INVALID_AMOUNT", "Số tiền phải lớn hơn 0");
        }
        if (amount.scale() > 2) {
            throw new TransferValidationException("INVALID_AMOUNT", "Số tiền tối đa 2 chữ số thập phân");
        }
        if (fromAccount.equals(toAccount)) {
            throw new TransferValidationException("SAME_ACCOUNT", "Tài khoản nguồn và đích phải khác nhau");
        }
        Instant now = Instant.now();
        return new Transfer(UUID.randomUUID(), idempotencyKey, fromAccount, toAccount, amount,
                TransferStatus.PENDING, null, now, now);
    }

    public void markDebited() {
        moveTo(TransferStatus.PENDING, TransferStatus.DEBITED);
    }

    public void complete() {
        moveTo(TransferStatus.DEBITED, TransferStatus.COMPLETED);
    }

    public void rejectDebit(String reason) {
        moveTo(TransferStatus.PENDING, TransferStatus.FAILED);
        this.failureReason = reason;
    }

    public void startCompensation(String reason) {
        moveTo(TransferStatus.DEBITED, TransferStatus.COMPENSATING);
        this.failureReason = reason;
    }

    public void finishCompensation() {
        moveTo(TransferStatus.COMPENSATING, TransferStatus.FAILED);
    }

    public boolean is(TransferStatus expected) {
        return status == expected;
    }

    private void moveTo(TransferStatus expected, TransferStatus next) {
        if (status != expected) {
            throw new IllegalStateException("Không thể chuyển giao dịch %s từ %s sang %s".formatted(id, status, next));
        }
        status = next;
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
