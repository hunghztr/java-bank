package com.bank.transaction.adapter.out.persistence;

import com.bank.transaction.application.port.out.TransferRepository;
import com.bank.transaction.domain.model.Transfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TransferPersistenceAdapter implements TransferRepository {

    private final TransferJpaRepository repository;

    TransferPersistenceAdapter(TransferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transfer save(Transfer t) {
        // Cập nhật lên entity đang quản lý (giữ version) thay vì tạo entity mới
        TransferJpaEntity e = repository.findById(t.getId()).orElseGet(TransferJpaEntity::new);
        e.id = t.getId();
        e.idempotencyKey = t.getIdempotencyKey();
        e.fromAccount = t.getFromAccount();
        e.toAccount = t.getToAccount();
        e.amount = t.getAmount();
        e.status = t.getStatus();
        e.failureReason = t.getFailureReason();
        e.createdAt = t.getCreatedAt();
        e.updatedAt = t.getUpdatedAt();
        return toDomain(repository.save(e));
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        return repository.findById(id).map(TransferPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(TransferPersistenceAdapter::toDomain);
    }

    private static Transfer toDomain(TransferJpaEntity e) {
        return new Transfer(e.id, e.idempotencyKey, e.fromAccount, e.toAccount, e.amount, e.status,
                e.failureReason, e.createdAt, e.updatedAt);
    }
}
