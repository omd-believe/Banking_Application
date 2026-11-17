package com.banking.account.service;

import com.banking.account.client.TransactionClient;
import com.banking.account.dto.AccountResponseDto;
import com.banking.account.dto.TransactionRequestDto;
import com.banking.account.entity.Account;
import com.banking.account.enums.TransactionType;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionClient transactionClient;

    public AccountService(AccountRepository accountRepository, TransactionClient transactionClient){
        this.accountRepository = accountRepository;
        this.transactionClient = transactionClient;
    }

    private AccountResponseDto mapToDto(Account account) {
        AccountResponseDto dto = new AccountResponseDto();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setBalance(account.getBalance());
        dto.setUserId(account.getUserId());
        return dto;
    }

    public AccountResponseDto create(Long userId) {

        Account newAccount = new Account();
        newAccount.setAccountNumber(UUID.randomUUID().toString());
        newAccount.setBalance(0.00);
        newAccount.setUserId(userId);

        Account savedAccount = accountRepository.save(newAccount);
        return mapToDto(savedAccount);

    }

    public List<AccountResponseDto> getAccount(Long userId) {
        List<Account> accounts = accountRepository.findAccountByUserId(userId);
        List<AccountResponseDto> dtos = new ArrayList<>();
        for (Account acc : accounts) {
            dtos.add(mapToDto(acc));
        }
        return dtos;
    }


    private void logTransaction(Long accountId, Double amount, TransactionType type, String description) {
        TransactionRequestDto log = new TransactionRequestDto();
        log.setAccountId(accountId);
        log.setAmount(amount);
        log.setTransactionType(type);
        log.setDescription(description);
        transactionClient.recordTransaction(log);
    }



    public AccountResponseDto deposit(Long accountId, Double amount) {
        Account account = accountRepository.findById(accountId).
                orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        account.setBalance(account.getBalance() + amount);
        Account savedAccount = accountRepository.save(account);

        logTransaction(accountId, amount, TransactionType.DEPOSIT, "Deposit at branch/ATM");

        return mapToDto(savedAccount);
    }


    public AccountResponseDto withdraw(Long accountId, Double amount) {
        Account account = accountRepository.findById(accountId).
                orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        if (account.getBalance() < amount) {
            throw new RuntimeException("Insufficient funds for withdrawal");
        }

        account.setBalance(account.getBalance() - amount);
        Account savedAccount = accountRepository.save(account);

        logTransaction(accountId, amount, TransactionType.WITHDRAWAL, "Withdrawal from account");

        return mapToDto(savedAccount);
    }


    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, Double amount) {
        withdraw(fromAccountId, amount);
        deposit(toAccountId, amount);
    }
}
