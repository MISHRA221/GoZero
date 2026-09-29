package com.gozero.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gozero.services")
public record ServiceProperties(
        String ingestionUrl,
        String sentimentUrl,
        String reconciliationUrl,
        int connectTimeoutMs,
        int readTimeoutMs) {
}
