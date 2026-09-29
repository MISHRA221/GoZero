package com.gozero.ingestion.scraper;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gozero.scraper")
public record ScraperProperties(
        boolean enabled,
        String baseUrl,
        String brandPath,
        String userAgent,
        long delayMs,
        int maxProducts,
        int timeoutMs,
        int minLiveProducts) {
}
