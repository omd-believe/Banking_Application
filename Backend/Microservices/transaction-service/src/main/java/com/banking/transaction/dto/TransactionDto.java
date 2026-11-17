package com.banking.transaction.dto;

import com.banking.transaction.enums.TransactionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionDto {
    private Long id;
    private Long accountId;
    private Double amount;
    private TransactionType transactionType;
    private String description;
    private LocalDateTime timestamp;
}