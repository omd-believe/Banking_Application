package com.banking.account.service;

import com.banking.account.entity.Account;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class AccountService {

    private AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    public Account create(Account account, Long userId) {

        Account newAccount = new Account();
        newAccount.setAccountNumber(UUID.randomUUID().toString());
        newAccount.setBalance(0.00);
        newAccount.setUserId(userId);

        return accountRepository.save(newAccount);

    }

    public List<Account> getAccount(Long userId) {

        return accountRepository.findAccountByUserId(userId);
    }

    public Account deposit(Long accountId, Double amount) {

        Account account = accountRepository.findById(accountId).
                orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
        account.setBalance(account.getBalance() + amount);

        return accountRepository.save(account);
    }

    public Account withdraw(Long accountId, Double amount) {

        Account account = accountRepository.findById(accountId).
                orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
        account.setBalance(account.getBalance() - amount);

        return accountRepository.save(account);
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
