package com.banking.account.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountResponseDto {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private Long userId;
}