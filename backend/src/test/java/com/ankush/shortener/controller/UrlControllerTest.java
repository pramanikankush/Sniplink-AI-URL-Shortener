package com.ankush.shortener.controller;

import com.ankush.shortener.config.AppProperties;
import com.ankush.shortener.dto.ShortenResponse;
import com.ankush.shortener.dto.StatsResponse;
import com.ankush.shortener.exception.UnsafeUrlException;
import com.ankush.shortener.exception.UrlNotFoundException;
import com.ankush.shortener.service.UrlShortenerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({UrlController.class, StatsController.class, RedirectController.class})
class UrlControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean UrlShortenerService service;

    @TestConfiguration
    static class Config {
        @Bean
        AppProperties appProperties() {
            return new AppProperties(
                    "http://localhost:8080", 7,
                    new AppProperties.CorsProps("*"),
                    new AppProperties.SafetyProps(true, 0.75, "", ""),
                    new AppProperties.RateLimitProps(1000, 1000)
            );
        }
    }

    @Test
    void shorten_validUrl_returnsShortLink() throws Exception {
        when(service.shorten(anyString()))
                .thenReturn(new ShortenResponse("abc1234", "http://localhost:8080/abc1234", 0.1));

        mvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", "https://example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("abc1234"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc1234"))
                .andExpect(jsonPath("$.riskScore").value(0.1));
    }

    @Test
    void shorten_invalidUrl_returns400() throws Exception {
        mvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", "not-a-url"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shorten_unsafeUrl_returns422() throws Exception {
        when(service.shorten(anyString()))
                .thenThrow(new UnsafeUrlException("URL flagged as unsafe"));
        mvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", "http://evil.xyz"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void stats_returnsStats() throws Exception {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        when(service.stats("abc1234"))
                .thenReturn(new StatsResponse("abc1234", "https://example.com", 15, now));
        mvc.perform(get("/api/v1/stats/abc1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").value(15));
    }

    @Test
    void stats_notFound_returns404() throws Exception {
        when(service.stats(anyString())).thenThrow(new UrlNotFoundException("missing"));
        mvc.perform(get("/api/v1/stats/zzz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirect_returns302() throws Exception {
        when(service.resolveAndCount("abc1234")).thenReturn("https://example.com");
        mvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }
}
