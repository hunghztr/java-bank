package com.bank.account.application;

import com.bank.account.domain.Account;
import com.bank.account.domain.AccountRepository;
import com.bank.common.core.BusinessException;
import com.bank.common.core.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Account get(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản " + accountNumber));
    }

    @Transactional
    public Account open(String accountNumber, String ownerName, String currency) {
        if (accounts.existsByAccountNumber(accountNumber)) {
            throw new BusinessException("ACCOUNT_EXISTS", "Số tài khoản đã tồn tại");
        }
        return accounts.save(new Account(accountNumber, ownerName, currency));
    }
}
