package com.banking.account.service;

import com.banking.account.entity.Account;
import com.banking.account.exception.FraudAlertException;
import com.banking.account.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class FraudDetectionService {

    private static final String VELOCITY_PREFIX = "fraud:velocity:account:";
    private static final long WINDOW_MILLIS = 60_000L; // 60-second sliding window
    private static final int MAX_TXNS_PER_WINDOW = 3;
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("100000.00");

    private final StringRedisTemplate redisTemplate;
    private final AccountRepository accountRepository;

    public FraudDetectionService(StringRedisTemplate redisTemplate, AccountRepository accountRepository) {
        this.redisTemplate = redisTemplate;
        this.accountRepository = accountRepository;
    }

    public void evaluateOutgoingTransaction(Account account, BigDecimal amount) {
        if (account.isFrozen()) {
            throw new FraudAlertException("Account " + account.getId() + " is frozen due to suspicious activity.");
        }

        // Rule 1: High-value threshold check
        if (amount.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            log.warn("FRAUD ALERT: High-value transfer of {} blocked on account {}", amount, account.getId());
            throw new FraudAlertException("Transaction exceeds instant transfer limit of 100,000.00. Manual approval required.");
        }

        // Rule 2: Redis Sliding-Window Velocity Check (max 3 outgoing txns per 60s)
        String key = VELOCITY_PREFIX + account.getId();
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_MILLIS;

        // Remove timestamps older than 60 seconds
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count transactions in current 60-second window
        Long recentCount = redisTemplate.opsForZSet().zCard(key);
        if (recentCount != null && recentCount >= MAX_TXNS_PER_WINDOW) {
            account.setFrozen(true);
            accountRepository.save(account);
            log.error("FRAUD FREEZE: Velocity spike ({} txns in 60s) detected on account {}", recentCount + 1, account.getId());
            throw new FraudAlertException("Account frozen due to rapid transaction velocity (>3 transfers in 60s).");
        }

        // Record current transaction timestamp in Redis ZSET
        redisTemplate.opsForZSet().add(key, now + ":" + UUID.randomUUID(), now);
        redisTemplate.expire(key, Duration.ofMinutes(2));
    }

    public void clearVelocityWindow(Long accountId) {
        redisTemplate.delete(VELOCITY_PREFIX + accountId);
    }
}