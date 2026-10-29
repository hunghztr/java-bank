package com.bank.common.core;

import java.time.Instant;
import java.util.Map;

/** Định dạng lỗi thống nhất cho mọi service. */
public record ErrorResponse(String code, String message, Map<String, String> fieldErrors, Instant timestamp) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Map.of(), Instant.now());
    }
}
