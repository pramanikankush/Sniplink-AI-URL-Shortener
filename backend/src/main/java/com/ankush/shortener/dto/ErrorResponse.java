package com.ankush.shortener.dto;

import java.time.Instant;

public record ErrorResponse(String error, int status, Instant timestamp) {
    public ErrorResponse(String error, int status) {
        this(error, status, Instant.now());
    }
}
