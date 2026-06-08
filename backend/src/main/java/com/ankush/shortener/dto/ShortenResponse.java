package com.ankush.shortener.dto;

public record ShortenResponse(String code, String shortUrl, double riskScore) {}
