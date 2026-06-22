package com.banking.account.controller;

import com.banking.account.entity.Account;
import com.banking.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private AccountService accountService;
    public AccountController (AccountService accountService){
        this.accountService = accountService;
    }

    @PostMapping("/create/{userId}")
    public ResponseEntity<Account> create(@RequestBody Account account, @PathVariable Long userId){
        Account newAccount = accountService.create(account, userId);
        return new ResponseEntity<>(newAccount, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Account>> getAccount(@PathVariable Long userId){
        List<Account> accounts = accountService.getAccount(userId);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @PutMapping ("/deposit/{accountId}")
    public ResponseEntity<Account> deposit(@PathVariable Long accountId,@RequestParam Double amount){
        Account account = accountService.deposit(accountId, amount);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @PutMapping ("/withdraw/{accountId}")
    public ResponseEntity<Account> withdraw(@PathVariable Long accountId,@RequestParam Double amount){
        Account account = accountService.withdraw(accountId, amount);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @PutMapping("/transfer/from/{fromAccountId}/to/{toAccountId}")
    public ResponseEntity<String> transfer(@PathVariable Long fromAccountId,
                                            @PathVariable Long toAccountId,
                                           @RequestParam Double amount){
        accountService.transfer(fromAccountId, toAccountId, amount);
        return ResponseEntity.ok().body("Transfer Successful!");
    }
}
