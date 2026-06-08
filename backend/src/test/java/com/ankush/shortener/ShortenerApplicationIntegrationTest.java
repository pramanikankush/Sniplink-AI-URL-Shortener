package com.ankush.shortener;

import com.ankush.shortener.dto.ShortenResponse;
import com.ankush.shortener.dto.StatsResponse;
import com.ankush.shortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ShortenerApplicationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shortener")
            .withUsername("shortener")
            .withPassword("shortener");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", pg::getJdbcUrl);
        reg.add("spring.datasource.username", pg::getUsername);
        reg.add("spring.datasource.password", pg::getPassword);
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired UrlShortenerService service;

    private String base() { return "http://localhost:" + port; }

    @Test
    void endToEnd_shortenThenRedirectThenStats() {
        ResponseEntity<ShortenResponse> created = rest.postForEntity(
                base() + "/api/v1/shorten",
                Map.of("url", "https://example.com/it-works"),
                ShortenResponse.class
        );
        assertEquals(HttpStatus.OK, created.getStatusCode());
        assertNotNull(created.getBody());
        ShortenResponse body = created.getBody();
        assertTrue(body.code().matches("[0-9A-Za-z]+"));
        assertTrue(body.riskScore() >= 0 && body.riskScore() <= 1);

        // Redirect returns 302 with Location header. We disable redirects on the
        // TestRestTemplate client by inspecting the raw response via the service.
        String destination = service.resolveAndCount(body.code());
        assertEquals("https://example.com/it-works", destination);

        // Stats reflect the click
        ResponseEntity<StatsResponse> stats = rest.getForEntity(
                base() + "/api/v1/stats/" + body.code(), StatsResponse.class);
        assertEquals(HttpStatus.OK, stats.getStatusCode());
        assertEquals(1, stats.getBody().clicks());
    }

    @Test
    void invalidUrl_rejected() {
        ResponseEntity<String> r = rest.postForEntity(
                base() + "/api/v1/shorten",
                Map.of("url", "not-a-url"),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void unknownCode_returns404() {
        ResponseEntity<String> r = rest.getForEntity(
                base() + "/api/v1/stats/doesnotexist", String.class);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
    }
}
