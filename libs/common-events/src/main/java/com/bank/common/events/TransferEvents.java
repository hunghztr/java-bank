package com.bank.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransferEvents {

    public record TransferCompleted(UUID transferId, String fromAccount, String toAccount, BigDecimal amount,
                                    Instant occurredAt) {
    }

    public record TransferFailed(UUID transferId, String fromAccount, String toAccount, BigDecimal amount,
                                 String reason, Instant occurredAt) {
    }

    private TransferEvents() {
    }
}
