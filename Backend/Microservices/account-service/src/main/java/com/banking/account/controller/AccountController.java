package com.banking.account.controller;

import com.banking.account.dto.AccountResponseDto;
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
    public ResponseEntity<AccountResponseDto> create(@PathVariable Long userId){
        return new ResponseEntity<>(accountService.create(userId), HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<AccountResponseDto>> getAccount(@PathVariable Long userId){
        return new ResponseEntity<>(accountService.getAccount(userId), HttpStatus.OK);
    }

    @PutMapping ("/deposit/{accountId}")
    public ResponseEntity<AccountResponseDto> deposit(@PathVariable Long accountId, @RequestParam Double amount){
        return new ResponseEntity<>(accountService.deposit(accountId, amount), HttpStatus.OK);
    }

    @PutMapping ("/withdraw/{accountId}")
    public ResponseEntity<AccountResponseDto> withdraw(@PathVariable Long accountId, @RequestParam Double amount){
        return new ResponseEntity<>(accountService.withdraw(accountId, amount), HttpStatus.OK);
    }

    @PutMapping("/transfer/from/{fromAccountId}/to/{toAccountId}")
    public ResponseEntity<String> transfer(@PathVariable Long fromAccountId,
                                           @PathVariable Long toAccountId,
                                           @RequestParam Double amount){
        accountService.transfer(fromAccountId, toAccountId, amount);
        return ResponseEntity.ok().body("Transfer Successful!");
    }
}
