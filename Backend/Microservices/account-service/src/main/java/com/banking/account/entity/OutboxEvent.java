package com.banking.account.entity;

import com.banking.account.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, createdAt")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId; // UUID for consumer deduplication

    @Column(nullable = false)
    private String aggregateType; // e.g., "ACCOUNT_TRANSACTION"

    @Column(nullable = false)
    private String aggregateId; // accountId

    @Column(nullable = false)
    private String topic; // "banking.transactions.events"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON serialized TransactionEventPayload

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    private int retryCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}