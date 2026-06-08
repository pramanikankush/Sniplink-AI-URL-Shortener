package com.ankush.shortener.controller;

import com.ankush.shortener.config.AppProperties;
import com.ankush.shortener.dto.StatsResponse;
import com.ankush.shortener.exception.UrlNotFoundException;
import com.ankush.shortener.service.UrlShortenerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

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
    void stats_returnsPayload() throws Exception {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        when(service.stats("abc1234"))
                .thenReturn(new StatsResponse("abc1234", "https://example.com", 7, now));
        mvc.perform(get("/api/v1/stats/abc1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("abc1234"))
                .andExpect(jsonPath("$.longUrl").value("https://example.com"))
                .andExpect(jsonPath("$.clicks").value(7));
    }

    @Test
    void stats_rejectsMalformedCode() throws Exception {
        when(service.stats(anyString())).thenThrow(new UrlNotFoundException("nope"));
        mvc.perform(get("/api/v1/stats/has.dot"))
                .andExpect(status().isNotFound());
    }
}
