package com.ankush.shortener.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "urls",
    indexes = {
        @Index(name = "idx_urls_long_url", columnList = "long_url")
    }
)
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private long clicks;

    public Url() {}

    public Url(String code, String longUrl) {
        this.code = code;
        this.longUrl = longUrl;
        this.createdAt = Instant.now();
        this.clicks = 0;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getLongUrl() { return longUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public long getClicks() { return clicks; }

    public void incrementClicks() { this.clicks++; }
}
