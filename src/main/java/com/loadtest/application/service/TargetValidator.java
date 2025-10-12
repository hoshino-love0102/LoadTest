package com.loadtest.application.service;

import com.loadtest.application.port.out.AllowedTargetRepository;

import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class TargetValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final AllowedTargetRepository allowedTargetRepository;
    private final Set<Integer> allowedPorts;

    public TargetValidator(AllowedTargetRepository allowedTargetRepository, List<Integer> allowedPorts) {
        this.allowedTargetRepository = allowedTargetRepository;
        this.allowedPorts = new HashSet<>(allowedPorts == null ? List.of() : allowedPorts);
    }

    public void validateOrThrow(String rawUrl) {
        ValidationResult r = validate(rawUrl);
        if (!r.allowed()) throw new TargetRejectedException(r.reason(), r.detail());
    }

    public ValidationResult validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return ValidationResult.block(TargetBlockReason.EMPTY_URL, "url is empty");
        }

        final URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            return ValidationResult.block(TargetBlockReason.INVALID_URL, e.getMessage());
        }

        String scheme = safeLower(uri.getScheme());
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme)) {
            return ValidationResult.block(TargetBlockReason.SCHEME_NOT_ALLOWED, "scheme=" + scheme);
        }

        if (uri.getUserInfo() != null) {
            return ValidationResult.block(TargetBlockReason.USERINFO_NOT_ALLOWED, "userinfo present");
        }

        String host = safeLower(uri.getHost());
        if (host == null || host.isBlank()) {
            return ValidationResult.block(TargetBlockReason.MISSING_HOST, "host is missing");
        }

        Set<String> allowedHosts = allowedTargetRepository.getAllowedHosts().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(this::safeLower)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());

        if (!allowedHosts.contains(host)) {
            return ValidationResult.block(TargetBlockReason.HOST_NOT_ALLOWED, "host=" + host);
        }

        int port = uri.getPort();
        if (port == -1) {
            port = scheme.equals("https") ? 443 : 80;
        }

        if (!allowedPorts.isEmpty() && !allowedPorts.contains(port)) {
            return ValidationResult.block(TargetBlockReason.PORT_NOT_ALLOWED, "port=" + port);
        }

        List<InetAddress> resolved;
        try {
            resolved = Arrays.asList(InetAddress.getAllByName(host));
        } catch (UnknownHostException e) {
            return ValidationResult.block(TargetBlockReason.DNS_LOOKUP_FAILED, e.getMessage());
        }

        for (InetAddress ip : resolved) {
            if (isBlockedAddress(ip)) {
                return ValidationResult.block(TargetBlockReason.PRIVATE_ADDRESS_BLOCKED, "resolved=" + ip.getHostAddress());
            }
        }

        return ValidationResult.allow("ok");
    }

    private boolean isBlockedAddress(InetAddress ip) {
        return ip.isAnyLocalAddress()
                || ip.isLoopbackAddress()
                || ip.isLinkLocalAddress()
                || ip.isSiteLocalAddress()
                || ip.isMulticastAddress();
    }

    private String safeLower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }
}
