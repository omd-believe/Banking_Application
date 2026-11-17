package com.banking.account.controller;

import com.banking.account.dto.AccountResponseDto;
import com.banking.account.service.AccountService;
import com.banking.account.service.IdempotencyService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final IdempotencyService idempotencyService;

    public AccountController(AccountService accountService, IdempotencyService idempotencyService) {
        this.accountService = accountService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/create/{userId}")
    public ResponseEntity<AccountResponseDto> create(@PathVariable Long userId) {
        return new ResponseEntity<>(accountService.create(userId), HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<AccountResponseDto>> getAccount(@PathVariable Long userId) {
        return new ResponseEntity<>(accountService.getAccount(userId), HttpStatus.OK);
    }

    @PutMapping("/deposit/{accountId}")
    public ResponseEntity<AccountResponseDto> deposit(
            @PathVariable Long accountId,
            @RequestParam @Positive BigDecimal amount,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        idempotencyService.acquireOrThrow(idempotencyKey);
        try {
            AccountResponseDto response = accountService.deposit(accountId, amount, idempotencyKey);
            idempotencyService.markCompleted(idempotencyKey);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            idempotencyService.releaseOnFailure(idempotencyKey);
            throw ex;
        }
    }

    @PutMapping("/withdraw/{accountId}")
    public ResponseEntity<AccountResponseDto> withdraw(
            @PathVariable Long accountId,
            @RequestParam @Positive BigDecimal amount,
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        idempotencyService.acquireOrThrow(idempotencyKey);
        try {
            AccountResponseDto response = accountService.withdraw(accountId, amount, authenticatedUserId, idempotencyKey);
            idempotencyService.markCompleted(idempotencyKey);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            idempotencyService.releaseOnFailure(idempotencyKey);
            throw ex;
        }
    }

    @PutMapping("/transfer/from/{fromAccountId}/to/{toAccountId}")
    public ResponseEntity<String> transfer(
            @PathVariable Long fromAccountId,
            @PathVariable Long toAccountId,
            @RequestParam @Positive BigDecimal amount,
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        idempotencyService.acquireOrThrow(idempotencyKey);
        try {
            accountService.transfer(fromAccountId, toAccountId, amount, authenticatedUserId, idempotencyKey);
            idempotencyService.markCompleted(idempotencyKey);
            return ResponseEntity.ok("Transfer Successful!");
        } catch (RuntimeException ex) {
            idempotencyService.releaseOnFailure(idempotencyKey);
            throw ex;
        }
    }
}