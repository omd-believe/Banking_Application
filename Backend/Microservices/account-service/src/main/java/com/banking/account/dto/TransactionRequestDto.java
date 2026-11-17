package com.banking.account.dto;

import com.banking.account.enums.TransactionType;
import lombok.Data;

@Data
public class TransactionRequestDto {
    private Long accountId;
    private Double amount;
    private TransactionType transactionType;
    private String description;
}
