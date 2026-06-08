package com.ankush.shortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed app configuration. Bound from {@code app.*} in application.yml.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        int codeLength,
        CorsProps cors,
        SafetyProps safety,
        RateLimitProps rateLimit
) {
    public record CorsProps(String allowedOrigins) {}
    public record SafetyProps(boolean enabled, double rejectThreshold, String modelPath, String vocabPath) {}
    public record RateLimitProps(int capacity, int refillPerMinute) {}
}
