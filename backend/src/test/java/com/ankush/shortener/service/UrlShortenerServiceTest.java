package com.ankush.shortener.service;

import com.ankush.shortener.config.AppProperties;
import com.ankush.shortener.dto.ShortenResponse;
import com.ankush.shortener.dto.StatsResponse;
import com.ankush.shortener.entity.Url;
import com.ankush.shortener.exception.UnsafeUrlException;
import com.ankush.shortener.exception.UrlNotFoundException;
import com.ankush.shortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UrlShortenerServiceTest {

    private UrlRepository repo;
    private Base62Encoder encoder;
    private UrlSafetyService safety;
    private AppProperties props;
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        repo = mock(UrlRepository.class);
        encoder = mock(Base62Encoder.class);
        safety = mock(UrlSafetyService.class);
        props = new AppProperties(
                "http://localhost:8080", 7,
                new AppProperties.CorsProps("*"),
                new AppProperties.SafetyProps(true, 0.75, "", ""),
                new AppProperties.RateLimitProps(20, 10)
        );
        service = new UrlShortenerService(repo, encoder, safety, props);
    }

    @Test
    void shorten_rejectsWhenAboveThreshold() {
        when(safety.score(anyString())).thenReturn(0.9);
        assertThrows(UnsafeUrlException.class,
                () -> service.shorten("https://evil.example/path"));
        verify(repo, never()).save(any());
    }

    @Test
    void shorten_isIdempotentForSameUrl() {
        when(safety.score(anyString())).thenReturn(0.1);
        Url existing = new Url("abc1234", "https://example.com");
        when(repo.findByLongUrl("https://example.com")).thenReturn(Optional.of(existing));
        ShortenResponse resp = service.shorten("https://example.com");
        assertEquals("abc1234", resp.code());
        assertEquals(0.1, resp.riskScore());
        assertEquals("http://localhost:8080/abc1234", resp.shortUrl());
        verify(repo, never()).save(any());
    }

    @Test
    void shorten_generatesCodeAndPersists() {
        when(safety.score(anyString())).thenReturn(0.0);
        when(repo.findByLongUrl(anyString())).thenReturn(Optional.empty());
        when(repo.existsByCode("code1")).thenReturn(false);
        when(encoder.next()).thenReturn("code1");
        when(repo.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortenResponse resp = service.shorten("https://example.com");
        assertEquals("code1", resp.code());
        ArgumentCaptor<Url> cap = ArgumentCaptor.forClass(Url.class);
        verify(repo).save(cap.capture());
        assertEquals("https://example.com", cap.getValue().getLongUrl());
        assertEquals("code1", cap.getValue().getCode());
        assertNotNull(resp.shortUrl());
    }

    @Test
    void shorten_handlesCollision() {
        when(safety.score(anyString())).thenReturn(0.0);
        when(repo.findByLongUrl(anyString())).thenReturn(Optional.empty());
        when(repo.existsByCode("dup")).thenReturn(true);
        when(repo.existsByCode("ok")).thenReturn(false);
        when(encoder.next()).thenReturn("dup", "ok");
        when(repo.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortenResponse resp = service.shorten("https://example.com");
        assertEquals("ok", resp.code());
    }

    @Test
    void shorten_rejectsInvalidUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> service.shorten("not a url"));
    }

    @Test
    void shorten_rejectsUrlExceedingMaxLength() {
        String tooLong = "https://example.com/" + "a".repeat(2050);
        assertThrows(IllegalArgumentException.class, () -> service.shorten(tooLong));
    }

    @Test
    void resolveAndCount_throwsOnMissingCode() {
        when(repo.findByCode("nope")).thenReturn(Optional.empty());
        assertThrows(UrlNotFoundException.class, () -> service.resolveAndCount("nope"));
    }

    @Test
    void resolveAndCount_rejectsInvalidCodeCharacters() {
        assertThrows(UrlNotFoundException.class, () -> service.resolveAndCount("../etc/passwd"));
        assertThrows(UrlNotFoundException.class, () -> service.resolveAndCount(""));
        assertThrows(UrlNotFoundException.class, () -> service.resolveAndCount(null));
    }

    @Test
    void resolveAndCount_incrementsAndReturns() {
        Url u = new Url("ok1", "https://example.com/x");
        when(repo.findByCode("ok1")).thenReturn(Optional.of(u));
        when(repo.incrementClicks("ok1")).thenReturn(1);
        assertEquals("https://example.com/x", service.resolveAndCount("ok1"));
        verify(repo).incrementClicks("ok1");
    }

    @Test
    void stats_throwsOnMissingCode() {
        when(repo.findByCode("nope")).thenReturn(Optional.empty());
        assertThrows(UrlNotFoundException.class, () -> service.stats("nope"));
    }

    @Test
    void stats_returnsMapping() {
        Url u = new Url("ok1", "https://example.com");
        when(repo.findByCode("ok1")).thenReturn(Optional.of(u));
        StatsResponse s = service.stats("ok1");
        assertEquals("ok1", s.code());
        assertEquals("https://example.com", s.longUrl());
        assertEquals(0, s.clicks());
        assertNotNull(s.createdAt());
    }
}
