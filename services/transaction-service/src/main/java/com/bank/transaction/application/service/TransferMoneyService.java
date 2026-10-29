package com.bank.transaction.application.service;

import com.bank.common.core.NotFoundException;
import com.bank.common.events.AccountCommands.DebitAccount;
import com.bank.common.events.Topics;
import com.bank.transaction.application.port.in.GetTransferQuery;
import com.bank.transaction.application.port.in.TransferMoneyUseCase;
import com.bank.transaction.application.port.out.MessagePublisher;
import com.bank.transaction.application.port.out.TransferRepository;
import com.bank.transaction.domain.model.Transfer;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferMoneyService implements TransferMoneyUseCase, GetTransferQuery {

    private final TransferRepository transfers;
    private final MessagePublisher publisher;

    public TransferMoneyService(TransferRepository transfers, MessagePublisher publisher) {
        this.transfers = transfers;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public Transfer transfer(TransferCommand cmd) {
        return transfers.findByIdempotencyKey(cmd.idempotencyKey()).orElseGet(() -> {
            Transfer transfer = transfers.save(
                    Transfer.create(cmd.idempotencyKey(), cmd.fromAccount(), cmd.toAccount(), cmd.amount()));
            // Bước 1 của saga: yêu cầu trừ tiền tài khoản nguồn
            publisher.publish(Topics.ACCOUNT_COMMANDS, transfer.getId().toString(),
                    new DebitAccount(transfer.getId(), transfer.getFromAccount(), transfer.getAmount()));
            return transfer;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Transfer get(UUID transferId) {
        return transfers.findById(transferId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch " + transferId));
    }
}
