package com.ankush.shortener.dto;

import java.time.Instant;

public record StatsResponse(String code, String longUrl, long clicks, Instant createdAt) {}
