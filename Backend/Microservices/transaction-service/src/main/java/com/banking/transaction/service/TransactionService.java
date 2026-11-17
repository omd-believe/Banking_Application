package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionDto;
import com.banking.transaction.dto.TransactionRequestDto;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    public TransactionService (TransactionRepository transactionRepository){
        this.transactionRepository = transactionRepository;
    }

    private TransactionDto mapToDto(Transaction transaction){
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setAccountId(transaction.getAccountId());
        dto.setAmount(transaction.getAmount());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setDescription(transaction.getDescription());
        dto.setTimestamp(transaction.getTimestamp());
        return dto;
    }

    public TransactionDto recordTransaction(TransactionRequestDto requestDto){
        Transaction transaction = new Transaction();
        transaction.setAccountId(requestDto.getAccountId());
        transaction.setAmount(requestDto.getAmount());
        transaction.setTransactionType(requestDto.getTransactionType());
        transaction.setDescription(requestDto.getDescription());

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToDto(savedTransaction);
    }

    public List<TransactionDto> getTransactionHistory(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);

        List<TransactionDto> dtos = new ArrayList<>();
        for (Transaction t : transactions) {
            dtos.add(mapToDto(t));
        }
        return dtos;
    }
}
