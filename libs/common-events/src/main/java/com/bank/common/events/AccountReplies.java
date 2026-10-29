package com.bank.common.events;

import java.util.UUID;

public final class AccountReplies {

    public record AccountDebited(UUID transferId) {
    }

    public record AccountDebitFailed(UUID transferId, String reason) {
    }

    public record AccountCredited(UUID transferId) {
    }

    public record AccountCreditFailed(UUID transferId, String reason) {
    }

    public record AccountRefunded(UUID transferId) {
    }

    private AccountReplies() {
    }
}
