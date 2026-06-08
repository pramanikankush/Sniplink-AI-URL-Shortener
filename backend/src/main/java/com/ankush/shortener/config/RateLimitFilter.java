package com.ankush.shortener.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP token-bucket rate limiter backed by Bucket4j.
 * Buckets are kept in-memory; appropriate for a single-node deployment.
 *
 * Applies to all {@code /api/**} traffic so an attacker cannot probe the stats
 * endpoint without bound. The cap is generous for read paths in production
 * deployments (override with {@code app.rate-limit.capacity} / {@code refill}).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AppProperties props;

    public RateLimitFilter(AppProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // Always allow CORS preflight through.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} path={}", ip, uri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            int retryAfter = Math.max(1, 60 / Math.max(1, props.rateLimit().refillPerMinute()));
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"status\":429}");
        }
    }

    private Bucket newBucket(String ignored) {
        AppProperties.RateLimitProps rl = props.rateLimit();
        Bandwidth limit = Bandwidth.builder()
                .capacity(Math.max(1, rl.capacity()))
                .refillIntervally(Math.max(1, rl.refillPerMinute()), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For is a comma-separated list; the first entry is the
            // original client. We trust the header in single-node deployments;
            // in front of a trusted reverse proxy, override at the proxy layer.
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
