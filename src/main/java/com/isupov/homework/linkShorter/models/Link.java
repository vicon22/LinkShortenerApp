package com.isupov.homework.linkShorter.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Link {
    private final String originalUrl;
    private final String shortUrl;
    private final long createdAt;
    private final long expiryTime;
    private int clickCount;

    @Setter
    private int maxClicks;

    public Link(String originalUrl, String shortUrl, long expiryTime, int maxClicks) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.createdAt = System.currentTimeMillis();
        this.expiryTime = expiryTime;
        this.clickCount = 0;
        this.maxClicks = maxClicks;
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public boolean isLimitReached() {
        return clickCount >= maxClicks;
    }
}