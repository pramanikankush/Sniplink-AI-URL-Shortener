package com.ankush.shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShortenRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        @Pattern(
                regexp = "^https?://.*",
                message = "url must start with http:// or https://"
        )
        String url
) {}
