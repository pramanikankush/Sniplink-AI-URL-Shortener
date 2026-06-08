package com.ankush.shortener.service;

import com.ankush.shortener.config.AppProperties;
import com.ankush.shortener.dto.ShortenResponse;
import com.ankush.shortener.dto.StatsResponse;
import com.ankush.shortener.entity.Url;
import com.ankush.shortener.exception.UnsafeUrlException;
import com.ankush.shortener.exception.UrlNotFoundException;
import com.ankush.shortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    /** Valid short codes: Base62 (digits + ASCII letters), 1-12 chars. */
    private static final Pattern CODE_PATTERN = Pattern.compile("[0-9A-Za-z]{1,12}");

    private final UrlRepository repository;
    private final Base62Encoder encoder;
    private final UrlSafetyService safety;
    private final AppProperties props;

    public UrlShortenerService(UrlRepository repository,
                               Base62Encoder encoder,
                               UrlSafetyService safety,
                               AppProperties props) {
        this.repository = repository;
        this.encoder = encoder;
        this.safety = safety;
        this.props = props;
    }

    @Transactional
    public ShortenResponse shorten(String rawUrl) {
        String normalized = normalize(rawUrl);
        double risk = safety.score(normalized);
        if (risk >= props.safety().rejectThreshold()) {
            log.info("Rejected unsafe URL risk={} url={}", risk, normalized);
            throw new UnsafeUrlException("URL flagged as unsafe (risk=" + risk + ")");
        }

        // Idempotent: return the existing code for the same long URL.
        Optional<Url> existing = repository.findByLongUrl(normalized);
        if (existing.isPresent()) {
            Url u = existing.get();
            return new ShortenResponse(u.getCode(), shortUrl(u.getCode()), risk);
        }

        // Generate a unique code (collision loop — extremely unlikely but safe).
        String code;
        int attempts = 0;
        do {
            code = encoder.next();
            attempts++;
        } while (repository.existsByCode(code) && attempts < 5);

        Url saved = repository.save(new Url(code, normalized));
        log.info("Created short link code={} risk={}", saved.getCode(), risk);
        return new ShortenResponse(saved.getCode(), shortUrl(saved.getCode()), risk);
    }

    @Transactional
    public String resolveAndCount(String code) {
        String safeCode = validateCode(code);
        Url url = repository.findByCode(safeCode)
                .orElseThrow(() -> new UrlNotFoundException("Short link not found: " + safeCode));
        int updated = repository.incrementClicks(safeCode);
        if (updated == 0) {
            // The row was deleted between the read and the increment (extremely rare).
            log.warn("Click increment vanished for code={}", safeCode);
        }
        return url.getLongUrl();
    }

    public StatsResponse stats(String code) {
        String safeCode = validateCode(code);
        Url url = repository.findByCode(safeCode)
                .orElseThrow(() -> new UrlNotFoundException("Short link not found: " + safeCode));
        return new StatsResponse(url.getCode(), url.getLongUrl(), url.getClicks(), url.getCreatedAt());
    }

    private String shortUrl(String code) {
        String base = props.baseUrl();
        if (base == null || base.isBlank()) base = "http://localhost:8080";
        return base + "/" + code;
    }

    private String normalize(String url) {
        String trimmed = url.trim();
        if (trimmed.length() > 2048) {
            throw new IllegalArgumentException("URL too long (max 2048 characters)");
        }
        try {
            URI uri = new URI(trimmed);
            // Re-parse to reject malformed hosts (uses the modern URI->URL path).
            uri.toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL: " + trimmed);
        }
        return trimmed;
    }

    private String validateCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new UrlNotFoundException("Short link not found");
        }
        return code;
    }
}
