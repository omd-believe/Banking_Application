package com.banking.transaction.dto;

import com.banking.transaction.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class TransactionRequestDto {

    @NotNull
    private Long accountId;

    @NotNull
    @Positive
    private Double amount;

    @NotNull
    private TransactionType transactionType;

    private String description;

}
