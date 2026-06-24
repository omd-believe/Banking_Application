package com.banking.account.service;

import com.banking.account.dto.AccountResponseDto;
import com.banking.account.entity.Account;
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

    private AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
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

    public AccountResponseDto deposit(Long accountId, Double amount) {

        Account account = accountRepository.findById(accountId).
                orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
        account.setBalance(account.getBalance() + amount);

        Account savedAccount = accountRepository.save(account);

        return mapToDto(savedAccount);
    }

    public AccountResponseDto withdraw(Long accountId, Double amount) {

        Account account = accountRepository.findById(accountId).
                orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
        account.setBalance(account.getBalance() - amount);

        Account savedAccount = accountRepository.save(account);

        return mapToDto(savedAccount);
    }


    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, Double amount) {
        Account sendingAccount = accountRepository.findById(fromAccountId).
                orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + fromAccountId));
        Account recievingAccount = accountRepository.findById(toAccountId).
                orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + toAccountId));
        withdraw(fromAccountId, amount);
        deposit(toAccountId, amount);
    }
}
