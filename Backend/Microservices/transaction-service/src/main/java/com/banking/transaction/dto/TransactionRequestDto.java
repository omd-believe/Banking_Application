package com.banking.transaction.dto;

import com.banking.transaction.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class TransactionRequestDto {

    @NotNull
    private Long accountId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionType transactionType;

    private String description;

}
