package com.bank.transaction.application.port.in;

import com.bank.transaction.domain.model.Transfer;
import java.math.BigDecimal;

public interface TransferMoneyUseCase {

    /** Tạo lệnh chuyển tiền; gọi lại với cùng idempotencyKey trả về giao dịch cũ, không tạo mới. */
    Transfer transfer(TransferCommand command);

    record TransferCommand(String idempotencyKey, String fromAccount, String toAccount, BigDecimal amount) {
    }
}
