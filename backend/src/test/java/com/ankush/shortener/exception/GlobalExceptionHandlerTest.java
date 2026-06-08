package com.ankush.shortener.exception;

import com.ankush.shortener.config.AppProperties;
import com.ankush.shortener.dto.ErrorResponse;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {com.ankush.shortener.controller.StatsController.class,
                            com.ankush.shortener.controller.UrlController.class})
class GlobalExceptionHandlerTest {

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
    void notFound_returnsStructuredBody() throws Exception {
        when(service.stats(anyString())).thenThrow(new UrlNotFoundException("missing"));
        mvc.perform(get("/api/v1/stats/zzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("missing"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unsafe_returns422() throws Exception {
        when(service.shorten(anyString())).thenThrow(new UnsafeUrlException("bad"));
        mvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(java.util.Map.of("url", "https://evil.xyz"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void validation_returns400WithFieldDetails() throws Exception {
        mvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(java.util.Map.of("url", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("url")));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest());
    }
}
