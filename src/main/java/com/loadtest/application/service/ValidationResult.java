package com.loadtest.application.service;

public record ValidationResult(
        boolean allowed,
        TargetBlockReason reason,
        String detail
) {
    public static ValidationResult allow(String detail) {
        return new ValidationResult(true, null, detail);
    }

    public static ValidationResult block(TargetBlockReason reason, String detail) {
        return new ValidationResult(false, reason, detail);
    }
}
