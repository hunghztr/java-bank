package com.bank.account.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    /** Khoá dòng (SELECT ... FOR UPDATE) khi thay đổi số dư để tránh hai giao dịch trừ tiền đồng thời */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findForUpdate(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
