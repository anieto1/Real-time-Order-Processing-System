package com.pm.inventoryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.inventoryservice.model.DeadLetterEvent;
import com.pm.inventoryservice.model.OutboxEvent;
import com.pm.inventoryservice.model.ProcessResult;
import com.pm.inventoryservice.repository.DeadLetterEventRepository;
import com.pm.inventoryservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutBoxProcessor {
    private final OutboxEventRepository outboxEventRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;
    private static final String MAIN_TOPIC = "inventory-events";
    private static final String DLQ_TOPIC = "inventory-events-dlq";

    @Scheduled(fixedRate = 5000)
    public void process() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt();

        if (events.isEmpty()) {
            log.debug("No outbox events found");
            return;
        }

        log.info("Processing {} unpublished events", events.size());
        int successCount = 0;
        int failureCount = 0;
        int dlqCount = 0;

        for(OutboxEvent event : events) {
            ProcessResult result = processEvent(event);
            switch (result) {
                case SUCCESS -> successCount++;
                case FAILED -> failureCount++;
                case MOVED_TO_DLQ -> dlqCount++;
            }
        }

        if (successCount > 0 || failureCount > 0 || dlqCount > 0) {
            log.info("Outbox processing complete - Success: {}, Failed: {}, DLQ: {}",
                    successCount, failureCount, dlqCount);
        }
    }

    @Transactional
    protected ProcessResult processEvent(OutboxEvent event) {
        try {
            kafkaTemplate.send(MAIN_TOPIC,
                    event.getAggregateId().toString(),
                    event.getPayload())
                .get();

            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);

            log.debug("Successfully published event: {}", event.getEventId());
            return ProcessResult.SUCCESS;

        } catch(Exception e) {
            if(event.getRetryCount() >= MAX_RETRIES) {
                moveToDeadLetterQueue(event, e);
                log.error("Event {} moved to DLQ after {} retries",
                        event.getEventId(), event.getRetryCount());
                return ProcessResult.MOVED_TO_DLQ;
            } else {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
                log.warn("Failed to publish event: {} (retry {}/{})",
                        event.getEventId(), event.getRetryCount(), MAX_RETRIES, e);
                return ProcessResult.FAILED;
            }
        }
    }

    @Transactional
    protected void moveToDeadLetterQueue(OutboxEvent event, Exception exception) {
        try {
            kafkaTemplate.send(DLQ_TOPIC,
                    event.getAggregateId().toString(),
                    event.getPayload())
                .get();

            DeadLetterEvent dlqEvent = DeadLetterEvent.builder()
                    .originalEventId(event.getEventId())
                    .aggregateId(event.getAggregateId())
                    .eventType(event.getEventType())
                    .payload(event.getPayload())
                    .retryCount(event.getRetryCount())
                    .failureReason(exception.getMessage())
                    .resolved(false)
                    .build();

            deadLetterEventRepository.save(dlqEvent);
            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);

            log.info("Event {} moved to DLQ after {} retries. Reason: {}",
                    event.getEventId(), event.getRetryCount(), exception.getMessage());

        } catch (Exception dlqException) {
            log.error("CRITICAL: Failed to move event {} to DLQ! Marking as published to prevent infinite retries.",
                    event.getEventId(), dlqException);
            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        }
    }

}
