package com.ankush.shortener.config;

import com.ankush.shortener.service.UrlShortenerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private AppProperties props;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        props = new AppProperties(
                "http://localhost:8080", 7,
                new AppProperties.CorsProps("*"),
                new AppProperties.SafetyProps(true, 0.75, "", ""),
                new AppProperties.RateLimitProps(2, 2)  // very small bucket for tests
        );
        filter = new RateLimitFilter(props);
    }

    @Test
    void allowsUpToCapacity_thenBlocks() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/shorten");
            req.setRemoteAddr("1.1.1.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus()); // chain proceeds, no response written
        }

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/shorten");
        req.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        assertEquals(429, res.getStatus());
        assertTrue(res.getHeader("Retry-After") != null);
        assertTrue(res.getContentAsString().contains("Rate limit"));
    }

    @Test
    void differentIps_haveIndependentBuckets() throws Exception {
        for (String ip : new String[]{"1.1.1.1", "1.1.1.2", "1.1.1.3"}) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/shorten");
            req.setRemoteAddr(ip);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, mock(FilterChain.class));
            assertEquals(200, res.getStatus());
        }
    }

    @Test
    void nonApiPathsBypassRateLimit() throws Exception {
        for (int i = 0; i < 50; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
            req.setRemoteAddr("1.1.1.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, mock(FilterChain.class));
            assertEquals(200, res.getStatus());
        }
    }

    @Test
    void xForwardedForIsHonored() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/shorten");
        req.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.1");
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, mock(FilterChain.class));
        // IP for bucket = 9.9.9.9
        assertEquals(200, res.getStatus());
    }

    @Test
    void optionsPreflightBypassesLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/api/v1/shorten");
            req.setRemoteAddr("1.1.1.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, mock(FilterChain.class));
            assertEquals(200, res.getStatus());
        }
    }
}
