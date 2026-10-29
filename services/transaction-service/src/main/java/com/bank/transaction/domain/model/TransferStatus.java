package com.bank.transaction.domain.model;

/**
 * PENDING → DEBITED → COMPLETED                       (thành công)
 * PENDING → FAILED                                    (trừ tiền nguồn thất bại)
 * PENDING → DEBITED → COMPENSATING → FAILED           (cộng tiền đích thất bại, đã hoàn tiền nguồn)
 */
public enum TransferStatus {
    PENDING,
    DEBITED,
    COMPLETED,
    COMPENSATING,
    FAILED
}
