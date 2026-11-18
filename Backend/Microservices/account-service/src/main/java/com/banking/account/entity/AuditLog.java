package com.banking.account.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action; // DEPOSIT, WITHDRAWAL, TRANSFER, ACCOUNT_FROZEN

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 64)
    private String previousHash;

    @Column(nullable = false, unique = true, length = 64)
    private String currentHash;
}