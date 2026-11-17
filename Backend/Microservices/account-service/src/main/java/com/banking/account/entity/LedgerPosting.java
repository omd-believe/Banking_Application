package com.banking.account.entity;

import com.banking.account.enums.EntryType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "ledger_postings", indexes = {
        @Index(name = "idx_ledger_account_id", columnList = "accountId")
})
public class LedgerPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(nullable = false)
    private Long accountId; // 0L represents External Cash/Bank Vault for Deposits & Withdrawals

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType; // DEBIT or CREDIT

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}