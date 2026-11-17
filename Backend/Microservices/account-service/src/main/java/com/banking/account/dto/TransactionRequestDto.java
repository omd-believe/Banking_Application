package com.banking.account.dto;

import com.banking.account.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequestDto {
    private Long accountId;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String description;
}
