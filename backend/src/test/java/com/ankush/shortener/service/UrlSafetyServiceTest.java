package com.ankush.shortener.service;

import com.ankush.shortener.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlSafetyServiceTest {

    private UrlSafetyService newService(boolean enabled, double threshold) {
        return new UrlSafetyService(new AppProperties(
                "http://x", 7,
                new AppProperties.CorsProps("*"),
                new AppProperties.SafetyProps(enabled, threshold, "", ""),
                new AppProperties.RateLimitProps(10, 10)
        ));
    }

    @Test
    void benignUrlsScoreLow() {
        UrlSafetyService s = newService(true, 0.75);
        assertTrue(s.score("https://www.google.com/search?q=hi") < 0.3);
        assertTrue(s.score("https://en.wikipedia.org/wiki/Java") < 0.3);
    }

    @Test
    void suspiciousPatternsScoreHigh() {
        UrlSafetyService s = newService(true, 0.75);
        double ipHost = s.score("http://192.168.1.1/login");
        double typo = s.score("https://paypa1.com/signin");
        assertTrue(ipHost > 0.2);
        assertTrue(typo > 0.3);
    }

    @Test
    void disabledSafetyReturnsZero() {
        UrlSafetyService s = newService(false, 0.75);
        assertEquals(0.0, s.score("http://192.168.1.1/evil"));
    }

    @Test
    void scoreClampedToOne() {
        UrlSafetyService s = newService(true, 0.75);
        double score = s.score("http://192.168.1.1/paypa1.com/password=abc.tk/login.zip?x=" + "a".repeat(200));
        assertTrue(score >= 0 && score <= 1);
    }
}
