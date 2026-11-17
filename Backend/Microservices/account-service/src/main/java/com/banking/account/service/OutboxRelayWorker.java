package com.banking.account.service;

import com.banking.account.entity.OutboxEvent;
import com.banking.account.enums.OutboxStatus;
import com.banking.account.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OutboxRelayWorker {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelayWorker(OutboxEventRepository outboxEventRepository,
                             KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                // Partition key = aggregateId (accountId) to guarantee per-account event ordering
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get();
                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(LocalDateTime.now());
            } catch (Exception ex) {
                int attempts = event.getRetryCount() + 1;
                event.setRetryCount(attempts);
                if (attempts >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event {} moved to FAILED after {} retries", event.getEventId(), attempts, ex);
                } else {
                    log.warn("Failed to publish outbox event {} (attempt {}/{})", event.getEventId(), attempts, MAX_RETRIES);
                }
            }
            outboxEventRepository.save(event);
        }
    }
}