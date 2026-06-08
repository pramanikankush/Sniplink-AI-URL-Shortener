package com.ankush.shortener.controller;

import com.ankush.shortener.config.AppProperties;
import com.ankush.shortener.exception.UrlNotFoundException;
import com.ankush.shortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

    @Autowired MockMvc mvc;
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
    void validCode_redirects() throws Exception {
        when(service.resolveAndCount("abc1234")).thenReturn("https://example.com");
        mvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void apiPrefix_returns404() throws Exception {
        mvc.perform(get("/api")).andExpect(status().isNotFound());
    }

    @Test
    void staticAsset_returns404() throws Exception {
        mvc.perform(get("/favicon.ico")).andExpect(status().isNotFound());
    }

    @Test
    void missingCode_returns404() throws Exception {
        when(service.resolveAndCount("nope")).thenThrow(new UrlNotFoundException("nope"));
        mvc.perform(get("/nope")).andExpect(status().isNotFound());
    }

    @Test
    void invalidCharacters_returns404() throws Exception {
        mvc.perform(get("/abc.123")).andExpect(status().isNotFound());
    }
}
