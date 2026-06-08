package com.ankush.shortener.controller;

import com.ankush.shortener.service.UrlShortenerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Public redirect handler. Uses 302 (temporary) so click tracking stays accurate.
 *
 * Reserved path segments ({@code api}, {@code actuator}, {@code error}) and
 * anything containing a {@code .} (typical static asset extension) are passed
 * through with 404 so they don't shadow the SPA's routing.
 */
@RestController
public class RedirectController {

    private static final Pattern VALID_CODE = Pattern.compile("[0-9A-Za-z]{1,12}");

    private final UrlShortenerService service;

    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        if (code == null || code.isBlank()
                || code.contains(".")
                || code.startsWith("api")
                || code.startsWith("actuator")
                || code.equals("error")
                || !VALID_CODE.matcher(code).matches()) {
            return ResponseEntity.notFound().build();
        }
        String destination = service.resolveAndCount(code);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(destination));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
