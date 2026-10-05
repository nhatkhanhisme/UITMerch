package com.uitmerch.backend.common.service;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="rate_limit_buckets")
public class RateLimitBucket {
    @Id @Column(length=64) private String keyHash;
    private Instant windowStart;
    private int attempts;
    private Instant expiresAt;
}
