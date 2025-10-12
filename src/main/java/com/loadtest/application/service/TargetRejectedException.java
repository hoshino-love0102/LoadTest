package com.loadtest.application.service;

public class TargetRejectedException extends RuntimeException {

    private final TargetBlockReason reason;
    private final String detail;

    public TargetRejectedException(TargetBlockReason reason, String detail) {
        super(reason + (detail == null ? "" : (": " + detail)));
        this.reason = reason;
        this.detail = detail;
    }

    public TargetBlockReason reason() {
        return reason;
    }

    public String detail() {
        return detail;
    }
}
