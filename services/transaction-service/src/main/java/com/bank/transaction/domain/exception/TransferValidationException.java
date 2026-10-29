package com.bank.transaction.domain.exception;

import com.bank.common.core.BusinessException;

public class TransferValidationException extends BusinessException {

    public TransferValidationException(String code, String message) {
        super(code, message);
    }
}
