package com.bank.account.application;

import com.bank.account.domain.Account;
import com.bank.account.domain.AccountRepository;
import com.bank.account.domain.ProcessedMessage;
import com.bank.account.domain.ProcessedMessageRepository;
import com.bank.common.core.BusinessException;
import com.bank.common.events.AccountCommands.CreditAccount;
import com.bank.common.events.AccountCommands.DebitAccount;
import com.bank.common.events.AccountCommands.RefundAccount;
import com.bank.common.events.AccountReplies.AccountCreditFailed;
import com.bank.common.events.AccountReplies.AccountCredited;
import com.bank.common.events.AccountReplies.AccountDebitFailed;
import com.bank.common.events.AccountReplies.AccountDebited;
import com.bank.common.events.AccountReplies.AccountRefunded;
import com.bank.common.events.MessageCodec;
import com.bank.common.events.Topics;
import com.bank.common.messaging.outbox.OutboxWriter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xử lý lệnh từ saga chuyển tiền. Thay đổi số dư, đánh dấu đã xử lý và ghi reply vào outbox
 * trong CÙNG một transaction.
 */
@Service
public class AccountCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountCommandHandler.class);

    private final AccountRepository accounts;
    private final ProcessedMessageRepository processed;
    private final OutboxWriter outbox;

    public AccountCommandHandler(AccountRepository accounts, ProcessedMessageRepository processed, OutboxWriter outbox) {
        this.accounts = accounts;
        this.processed = processed;
        this.outbox = outbox;
    }

    @Transactional
    public void handle(Object command) {
        switch (command) {
            case DebitAccount c -> once(c.transferId(), c, () -> debit(c));
            case CreditAccount c -> once(c.transferId(), c, () -> credit(c));
            case RefundAccount c -> once(c.transferId(), c, () -> refund(c));
            default -> throw new IllegalArgumentException("Lệnh không hỗ trợ: " + command.getClass().getSimpleName());
        }
    }

    private void once(UUID transferId, Object command, Runnable action) {
        String messageId = transferId + ":" + MessageCodec.typeOf(command);
        if (processed.existsById(messageId)) {
            log.info("Bỏ qua lệnh trùng {}", messageId);
            return;
        }
        action.run();
        processed.save(new ProcessedMessage(messageId));
    }

    private void debit(DebitAccount c) {
        try {
            Account account = lock(c.accountNumber());
            account.debit(c.amount());
            reply(c.transferId(), new AccountDebited(c.transferId()));
        } catch (BusinessException e) {
            reply(c.transferId(), new AccountDebitFailed(c.transferId(), e.getMessage()));
        }
    }

    private void credit(CreditAccount c) {
        try {
            Account account = lock(c.accountNumber());
            account.credit(c.amount());
            reply(c.transferId(), new AccountCredited(c.transferId()));
        } catch (BusinessException e) {
            reply(c.transferId(), new AccountCreditFailed(c.transferId(), e.getMessage()));
        }
    }

    // Hoàn tiền không được phép thất bại về nghiệp vụ; lỗi kỹ thuật sẽ được Kafka retry rồi vào DLT
    private void refund(RefundAccount c) {
        lock(c.accountNumber()).credit(c.amount());
        reply(c.transferId(), new AccountRefunded(c.transferId()));
    }

    private Account lock(String accountNumber) {
        return accounts.findForUpdate(accountNumber)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản " + accountNumber));
    }

    private void reply(UUID transferId, Object reply) {
        outbox.write(Topics.ACCOUNT_REPLIES, transferId.toString(), reply);
    }
}
