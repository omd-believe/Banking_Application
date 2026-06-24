package com.banking.account.dto;

import lombok.Data;

@Data
public class AccountResponseDto {
    private Long id;
    private String accountNumber;
    private Double balance;
    private Long userId;
}