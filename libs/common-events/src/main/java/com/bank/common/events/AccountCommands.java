package com.bank.common.events;

import java.math.BigDecimal;
import java.util.UUID;

/** Lệnh gửi tới account-service. Mọi lệnh đều mang transferId để xử lý idempotent. */
public final class AccountCommands {

    public record DebitAccount(UUID transferId, String accountNumber, BigDecimal amount) {
    }

    public record CreditAccount(UUID transferId, String accountNumber, BigDecimal amount) {
    }

    /** Giao dịch bù trừ (compensation) của saga: hoàn tiền về tài khoản nguồn khi cộng tiền thất bại */
    public record RefundAccount(UUID transferId, String accountNumber, BigDecimal amount) {
    }

    private AccountCommands() {
    }
}
