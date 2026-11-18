package com.banking.account.service;

import com.banking.account.entity.AuditLog;
import com.banking.account.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class AuditChainService {

    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLogRepository auditLogRepository;

    public AuditChainService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void recordEvent(String action, Long accountId, String details) {
        String previousHash = auditLogRepository.findTopByOrderByIdDesc()
                .map(AuditLog::getCurrentHash)
                .orElse(GENESIS_HASH);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String payload = previousHash + "|" + action + "|" + accountId + "|" + details + "|" + now;
        String currentHash = sha256(payload);

        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setAccountId(accountId);
        entry.setDetails(details);
        entry.setTimestamp(now);
        entry.setPreviousHash(previousHash);
        entry.setCurrentHash(currentHash);

        auditLogRepository.save(entry);
    }

    public Map<String, Object> verifyChainIntegrity() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByIdAsc();
        String expectedPrevHash = GENESIS_HASH;

        for (AuditLog entry : logs) {
            if (!entry.getPreviousHash().equals(expectedPrevHash)) {
                return Map.of(
                        "valid", false,
                        "brokenAtLogId", entry.getId(),
                        "reason", "Previous hash mismatch — chain link broken"
                );
            }
            String recomputed = sha256(
                    entry.getPreviousHash() + "|" + entry.getAction() + "|" +
                            entry.getAccountId() + "|" + entry.getDetails() + "|" + entry.getTimestamp()
            );
            if (!entry.getCurrentHash().equals(recomputed)) {
                return Map.of(
                        "valid", false,
                        "brokenAtLogId", entry.getId(),
                        "reason", "Payload tampered — SHA-256 signature mismatch"
                );
            }
            expectedPrevHash = entry.getCurrentHash();
        }

        return Map.of(
                "valid", true,
                "totalVerifiedRecords", logs.size(),
                "latestHash", expectedPrevHash
        );
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 hashing failed", e);
        }
    }
}