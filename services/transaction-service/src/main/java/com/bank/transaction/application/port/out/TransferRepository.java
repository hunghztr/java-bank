package com.bank.transaction.application.port.out;

import com.bank.transaction.domain.model.Transfer;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {

    Transfer save(Transfer transfer);

    Optional<Transfer> findById(UUID id);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
