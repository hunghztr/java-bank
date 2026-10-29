package com.bank.common.core;

/** Lỗi nghiệp vụ có mã lỗi ổn định để client xử lý (trả về HTTP 400/422). */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
