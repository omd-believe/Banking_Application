package com.banking.account.service;

import com.banking.account.dto.AccountResponseDto;
import com.banking.account.dto.TransactionEventPayload;
import com.banking.account.entity.Account;
import com.banking.account.entity.JournalEntry;
import com.banking.account.entity.LedgerPosting;
import com.banking.account.entity.OutboxEvent;
import com.banking.account.enums.EntryType;
import com.banking.account.enums.OutboxStatus;
import com.banking.account.enums.TransactionType;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.exception.InsufficientFundsException;
import com.banking.account.exception.UnauthorizedAccountAccessException;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.JournalEntryRepository;
import com.banking.account.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private static final Long SYSTEM_VAULT_ACCOUNT_ID = 0L;

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final FraudDetectionService fraudDetectionService;
    private final AuditChainService auditChainService;

    public AccountService(AccountRepository accountRepository,
                          JournalEntryRepository journalEntryRepository,
                          OutboxEventRepository outboxEventRepository,
                          ObjectMapper objectMapper,
                          FraudDetectionService fraudDetectionService,
                          AuditChainService auditChainService) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.fraudDetectionService = fraudDetectionService;
        this.auditChainService = auditChainService;
    }

    public AccountResponseDto create(Long userId) {
        Account newAccount = new Account();
        newAccount.setAccountNumber(UUID.randomUUID().toString());
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setUserId(userId);
        return mapToDto(accountRepository.save(newAccount));
    }

    public List<AccountResponseDto> getAccount(Long userId) {
        return accountRepository.findAccountByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public AccountResponseDto deposit(Long accountId, BigDecimal amount, String referenceId) {
        validatePositiveAmount(amount);
        String resolvedRef = resolveReferenceId(referenceId);

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        Account savedAccount = accountRepository.save(account);

        // Double-entry: DEBIT System Cash Vault, CREDIT Customer Account
        JournalEntry entry = buildJournalEntry(resolvedRef, TransactionType.DEPOSIT, "Cash/ATM Deposit");
        entry.addPosting(createPosting(SYSTEM_VAULT_ACCOUNT_ID, EntryType.DEBIT, amount, BigDecimal.ZERO));
        entry.addPosting(createPosting(accountId, EntryType.CREDIT, amount, newBalance));
        journalEntryRepository.save(entry);

        auditChainService.recordEvent("DEPOSIT", accountId, "Amount=" + amount + ", Ref=" + resolvedRef);

        queueTransactionEvent(resolvedRef, accountId, SYSTEM_VAULT_ACCOUNT_ID,
                amount, TransactionType.DEPOSIT, "Deposit at branch/ATM");

        return mapToDto(savedAccount);
    }

    @Transactional
    public AccountResponseDto withdraw(Long accountId, BigDecimal amount, Long authenticatedUserId, String referenceId) {
        validatePositiveAmount(amount);
        String resolvedRef = resolveReferenceId(referenceId);

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        verifyOwnership(account, authenticatedUserId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }

        fraudDetectionService.evaluateOutgoingTransaction(account, amount);

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        Account savedAccount = accountRepository.save(account);

        // Double-entry: DEBIT Customer Account, CREDIT System Cash Vault
        JournalEntry entry = buildJournalEntry(resolvedRef, TransactionType.WITHDRAWAL, "Account Withdrawal");
        entry.addPosting(createPosting(accountId, EntryType.DEBIT, amount, newBalance));
        entry.addPosting(createPosting(SYSTEM_VAULT_ACCOUNT_ID, EntryType.CREDIT, amount, BigDecimal.ZERO));
        journalEntryRepository.save(entry);

        auditChainService.recordEvent("WITHDRAWAL", accountId, "Amount=" + amount + ", Ref=" + resolvedRef);

        queueTransactionEvent(resolvedRef, accountId, SYSTEM_VAULT_ACCOUNT_ID,
                amount, TransactionType.WITHDRAWAL, "Withdrawal from account");

        return mapToDto(savedAccount);
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                         Long authenticatedUserId, String referenceId) {
        validatePositiveAmount(amount);
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        String resolvedRef = resolveReferenceId(referenceId);

        // Deadlock-free locking: always lock lower account ID first
        Long firstLockId = Math.min(fromAccountId, toAccountId);
        Long secondLockId = Math.max(fromAccountId, toAccountId);

        Account firstLocked = accountRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + firstLockId));
        Account secondLocked = accountRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + secondLockId));

        Account fromAccount = fromAccountId.equals(firstLockId) ? firstLocked : secondLocked;
        Account toAccount = toAccountId.equals(firstLockId) ? firstLocked : secondLocked;

        verifyOwnership(fromAccount, authenticatedUserId);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for transfer");
        }

        fraudDetectionService.evaluateOutgoingTransaction(fromAccount, amount);

        BigDecimal senderNewBalance = fromAccount.getBalance().subtract(amount);
        BigDecimal receiverNewBalance = toAccount.getBalance().add(amount);

        fromAccount.setBalance(senderNewBalance);
        toAccount.setBalance(receiverNewBalance);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Double-entry: DEBIT Sender, CREDIT Receiver (Net sum = 0)
        JournalEntry entry = buildJournalEntry(resolvedRef, TransactionType.TRANSFER,
                "Transfer from account " + fromAccountId + " to " + toAccountId);
        entry.addPosting(createPosting(fromAccountId, EntryType.DEBIT, amount, senderNewBalance));
        entry.addPosting(createPosting(toAccountId, EntryType.CREDIT, amount, receiverNewBalance));
        journalEntryRepository.save(entry);

        auditChainService.recordEvent("TRANSFER", fromAccountId, "To=" + toAccountId + ", Amount=" + amount + ", Ref=" + resolvedRef);

        // Queue async Kafka events for both sender and receiver
        queueTransactionEvent(resolvedRef, fromAccountId, toAccountId,
                amount, TransactionType.TRANSFER, "Transferred to account " + toAccountId);
        queueTransactionEvent(resolvedRef, toAccountId, fromAccountId,
                amount, TransactionType.TRANSFER, "Received from account " + fromAccountId);
    }

    private void verifyOwnership(Account account, Long authenticatedUserId) {
        if (authenticatedUserId != null && !account.getUserId().equals(authenticatedUserId)) {
            throw new UnauthorizedAccountAccessException("You do not own account ID: " + account.getId());
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private String resolveReferenceId(String referenceId) {
        return (referenceId != null && !referenceId.isBlank()) ? referenceId : UUID.randomUUID().toString();
    }

    private JournalEntry buildJournalEntry(String referenceId, TransactionType type, String description) {
        JournalEntry entry = new JournalEntry();
        entry.setReferenceId(referenceId);
        entry.setTransactionType(type);
        entry.setDescription(description);
        return entry;
    }

    private LedgerPosting createPosting(Long accountId, EntryType type, BigDecimal amount, BigDecimal balanceAfter) {
        LedgerPosting posting = new LedgerPosting();
        posting.setAccountId(accountId);
        posting.setEntryType(type);
        posting.setAmount(amount);
        posting.setBalanceAfter(balanceAfter);
        return posting;
    }

    private void queueTransactionEvent(String referenceId, Long accountId, Long counterpartyAccountId,
                                       BigDecimal amount, TransactionType type, String description) {
        try {
            String eventId = UUID.randomUUID().toString();
            TransactionEventPayload payload = new TransactionEventPayload(
                    eventId,
                    referenceId,
                    accountId,
                    counterpartyAccountId,
                    amount,
                    type,
                    description,
                    LocalDateTime.now()
            );

            OutboxEvent outbox = new OutboxEvent();
            outbox.setEventId(eventId);
            outbox.setAggregateType("ACCOUNT_TRANSACTION");
            outbox.setAggregateId(String.valueOf(accountId));
            outbox.setTopic("banking.transactions.events");
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outbox.setStatus(OutboxStatus.PENDING);

            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox transaction event", e);
        }
    }

    private AccountResponseDto mapToDto(Account account) {
        AccountResponseDto dto = new AccountResponseDto();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setBalance(account.getBalance());
        dto.setUserId(account.getUserId());
        return dto;
    }

    @Transactional
    public void unfreezeAccount(Long accountId) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        account.setFrozen(false);
        accountRepository.save(account);

        // Clear the Redis velocity counter so the user can transact immediately
        fraudDetectionService.clearVelocityWindow(accountId);

        // Record the admin unfreeze action in the hash-chained audit log
        auditChainService.recordEvent("ACCOUNT_UNFROZEN", accountId, "Account unfrozen after manual review");
    }
}