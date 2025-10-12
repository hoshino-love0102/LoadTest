package com.loadtest.application.service;

public enum TargetBlockReason {
    EMPTY_URL,
    INVALID_URL,
    SCHEME_NOT_ALLOWED,
    USERINFO_NOT_ALLOWED,
    MISSING_HOST,
    HOST_NOT_ALLOWED,
    PORT_NOT_ALLOWED,
    DNS_LOOKUP_FAILED,
    PRIVATE_ADDRESS_BLOCKED
}
