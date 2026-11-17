package com.banking.account.dto;

import com.banking.account.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEventPayload {
    private String eventId;
    private String referenceId;
    private Long accountId;
    private Long counterpartyAccountId;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String description;
    private LocalDateTime occurredAt;
}