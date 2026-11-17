package com.banking.transaction.consumer;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.enums.TransactionType;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public TransactionEventConsumer(TransactionRepository transactionRepository, ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "banking.transactions.events", groupId = "transaction-service-group")
    @Transactional
    public void consumeTransactionEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventId = node.get("eventId").asText();

            // Idempotency check: skip if already processed
            if (transactionRepository.existsByEventId(eventId)) {
                log.info("Skipping duplicate Kafka event: {}", eventId);
                return;
            }

            Transaction transaction = new Transaction();
            transaction.setEventId(eventId);
            transaction.setAccountId(node.get("accountId").asLong());
            transaction.setAmount(new BigDecimal(node.get("amount").asText()));
            transaction.setTransactionType(TransactionType.valueOf(node.get("transactionType").asText()));
            transaction.setDescription(node.get("description").asText());

            transactionRepository.save(transaction);
            log.info("Persisted transaction from Kafka event {} for account {}", eventId, transaction.getAccountId());
        } catch (Exception e) {
            log.error("Error processing Kafka transaction event: {}", message, e);
            throw new RuntimeException("Kafka event processing failed", e);
        }
    }
}