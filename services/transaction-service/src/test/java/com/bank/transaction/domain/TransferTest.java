package com.bank.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.transaction.domain.exception.TransferValidationException;
import com.bank.transaction.domain.model.Transfer;
import com.bank.transaction.domain.model.TransferStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransferTest {

    private Transfer newTransfer() {
        return Transfer.create("key-12345678", "1000000001", "1000000002", new BigDecimal("150000.00"));
    }

    @Test
    void happyPath() {
        Transfer t = newTransfer();
        t.markDebited();
        t.complete();
        assertThat(t.getStatus()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void compensationWhenCreditFails() {
        Transfer t = newTransfer();
        t.markDebited();
        t.startCompensation("Tài khoản đích bị khoá");
        t.finishCompensation();
        assertThat(t.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(t.getFailureReason()).isEqualTo("Tài khoản đích bị khoá");
    }

    @Test
    void cannotCompleteBeforeDebit() {
        assertThatThrownBy(() -> newTransfer().complete()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsSameAccount() {
        assertThatThrownBy(() -> Transfer.create("key-12345678", "1000000001", "1000000001", BigDecimal.TEN))
                .isInstanceOf(TransferValidationException.class);
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> Transfer.create("key-12345678", "1000000001", "1000000002", BigDecimal.ZERO))
                .isInstanceOf(TransferValidationException.class);
    }
}
