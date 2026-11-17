package com.banking.account.service;

import com.banking.account.exception.DuplicateTransactionException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private static final String PREFIX = "idempotency:account:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void acquireOrThrow(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return; // Optional for backward compatibility; enforce required header in production
        }
        String key = PREFIX + idempotencyKey;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "IN_PROGRESS", TTL);
        if (Boolean.FALSE.equals(acquired)) {
            throw new DuplicateTransactionException("Duplicate request detected for X-Idempotency-Key: " + idempotencyKey);
        }
    }

    public void markCompleted(String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            redisTemplate.opsForValue().set(PREFIX + idempotencyKey, "COMPLETED", TTL);
        }
    }

    public void releaseOnFailure(String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            redisTemplate.delete(PREFIX + idempotencyKey);
        }
    }
}