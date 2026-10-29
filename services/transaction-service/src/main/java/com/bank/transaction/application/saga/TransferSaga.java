package com.bank.transaction.application.saga;

import com.bank.common.events.AccountCommands.CreditAccount;
import com.bank.common.events.AccountCommands.RefundAccount;
import com.bank.common.events.AccountReplies.AccountCreditFailed;
import com.bank.common.events.AccountReplies.AccountCredited;
import com.bank.common.events.AccountReplies.AccountDebitFailed;
import com.bank.common.events.AccountReplies.AccountDebited;
import com.bank.common.events.AccountReplies.AccountRefunded;
import com.bank.common.events.Topics;
import com.bank.common.events.TransferEvents.TransferCompleted;
import com.bank.common.events.TransferEvents.TransferFailed;
import com.bank.transaction.application.port.out.MessagePublisher;
import com.bank.transaction.application.port.out.TransferRepository;
import com.bank.transaction.domain.model.Transfer;
import com.bank.transaction.domain.model.TransferStatus;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga điều phối (orchestration) chuyển tiền giữa 2 tài khoản:
 * trừ nguồn → cộng đích → hoàn tất; nếu cộng đích lỗi thì hoàn tiền nguồn (giao dịch bù trừ).
 * Reply trùng hoặc đến muộn (không đúng trạng thái chờ) được bỏ qua để xử lý idempotent.
 */
@Component
public class TransferSaga {

    private static final Logger log = LoggerFactory.getLogger(TransferSaga.class);

    private final TransferRepository transfers;
    private final MessagePublisher publisher;

    public TransferSaga(TransferRepository transfers, MessagePublisher publisher) {
        this.transfers = transfers;
        this.publisher = publisher;
    }

    @Transactional
    public void on(Object reply) {
        switch (reply) {
            case AccountDebited r -> step(r.transferId(), TransferStatus.PENDING, t -> {
                t.markDebited();
                command(t, new CreditAccount(t.getId(), t.getToAccount(), t.getAmount()));
            });
            case AccountDebitFailed r -> step(r.transferId(), TransferStatus.PENDING, t -> {
                t.rejectDebit(r.reason());
                failed(t);
            });
            case AccountCredited r -> step(r.transferId(), TransferStatus.DEBITED, t -> {
                t.complete();
                publisher.publish(Topics.TRANSFER_EVENTS, t.getId().toString(), new TransferCompleted(
                        t.getId(), t.getFromAccount(), t.getToAccount(), t.getAmount(), Instant.now()));
            });
            case AccountCreditFailed r -> step(r.transferId(), TransferStatus.DEBITED, t -> {
                t.startCompensation(r.reason());
                command(t, new RefundAccount(t.getId(), t.getFromAccount(), t.getAmount()));
            });
            case AccountRefunded r -> step(r.transferId(), TransferStatus.COMPENSATING, t -> {
                t.finishCompensation();
                failed(t);
            });
            default -> throw new IllegalArgumentException("Reply không hỗ trợ: " + reply.getClass().getSimpleName());
        }
    }

    private void step(UUID transferId, TransferStatus expected, java.util.function.Consumer<Transfer> action) {
        Transfer transfer = transfers.findById(transferId).orElse(null);
        if (transfer == null) {
            log.warn("Reply cho giao dịch không tồn tại {}", transferId);
            return;
        }
        if (!transfer.is(expected)) {
            log.info("Bỏ qua reply trùng/muộn cho {} (đang {}, chờ {})", transferId, transfer.getStatus(), expected);
            return;
        }
        action.accept(transfer);
        transfers.save(transfer);
    }

    private void command(Transfer t, Object command) {
        publisher.publish(Topics.ACCOUNT_COMMANDS, t.getId().toString(), command);
    }

    private void failed(Transfer t) {
        publisher.publish(Topics.TRANSFER_EVENTS, t.getId().toString(), new TransferFailed(
                t.getId(), t.getFromAccount(), t.getToAccount(), t.getAmount(), t.getFailureReason(), Instant.now()));
    }
}
