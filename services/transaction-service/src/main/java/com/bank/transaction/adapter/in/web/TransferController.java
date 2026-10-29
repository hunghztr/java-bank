package com.bank.transaction.adapter.in.web;

import com.bank.transaction.application.port.in.GetTransferQuery;
import com.bank.transaction.application.port.in.TransferMoneyUseCase;
import com.bank.transaction.application.port.in.TransferMoneyUseCase.TransferCommand;
import com.bank.transaction.domain.model.Transfer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferMoneyUseCase transferMoney;
    private final GetTransferQuery getTransfer;

    public TransferController(TransferMoneyUseCase transferMoney, GetTransferQuery getTransfer) {
        this.transferMoney = transferMoney;
        this.getTransfer = getTransfer;
    }

    /** Trả 202 Accepted: giao dịch xử lý bất đồng bộ qua saga, client hỏi trạng thái bằng GET. */
    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @RequestHeader("Idempotency-Key") @Size(min = 8, max = 100) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        Transfer t = transferMoney.transfer(
                new TransferCommand(idempotencyKey, request.fromAccount(), request.toAccount(), request.amount()));
        return ResponseEntity.accepted().body(TransferResponse.from(t));
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable UUID id) {
        return TransferResponse.from(getTransfer.get(id));
    }

    public record TransferRequest(
            @NotBlank String fromAccount,
            @NotBlank String toAccount,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {
    }

    public record TransferResponse(UUID id, String fromAccount, String toAccount, BigDecimal amount, String status,
                                   String failureReason, Instant createdAt, Instant updatedAt) {
        static TransferResponse from(Transfer t) {
            return new TransferResponse(t.getId(), t.getFromAccount(), t.getToAccount(), t.getAmount(),
                    t.getStatus().name(), t.getFailureReason(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }
}
