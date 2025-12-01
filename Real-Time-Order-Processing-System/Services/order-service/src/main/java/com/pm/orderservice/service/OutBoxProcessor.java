package com.pm.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.orderservice.model.DeadLetterEvent;
import com.pm.orderservice.model.OutboxEvent;
import com.pm.orderservice.repository.DeadLetterEventRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
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
    private static final String MAIN_TOPIC = "order-events";

    @Transactional
    @Scheduled(fixedRate = 5000)
    public void process() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt();

        if (events.isEmpty()) {
            log.debug("No unpublished events found");
            return;
        }

        log.info("Processing {} unpublished events", events.size());
        int successCount = 0;
        int failureCount = 0;
        int dlqCount = 0;

        for(OutboxEvent event : events) {
            try {
                kafkaTemplate.send(MAIN_TOPIC,
                        event.getAggregateId().toString(),
                        event.getPayload());
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                successCount++;
                log.debug("Successfully published event: {}", event.getEventId());

            } catch(Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
                if(event.getRetryCount() > MAX_RETRIES) {
                    moveToDeadLetterQueue(event, e);
                    dlqCount++;
                    log.error("Event {} moved to DLQ after {} retries",
                        event.getEventId(), event.getRetryCount());
                } else {
                    failureCount++;
                    log.warn("Failed to publish event: {} (retry {}/{})",
                        event.getEventId(), event.getRetryCount(), MAX_RETRIES, e);
                }
            }
        }

        if (successCount > 0 || failureCount > 0 || dlqCount > 0) {
            log.info("Outbox processing complete - Success: {}, Failed: {}, DLQ: {}",
                successCount, failureCount, dlqCount);
        }
    }

    private void moveToDeadLetterQueue(OutboxEvent event, Exception exception) {
        try {
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

            log.error("Event {} moved to DLQ after {} retries. Reason: {}",
                event.getEventId(), event.getRetryCount(), exception.getMessage());

        } catch (Exception dlqException) {
            log.error("CRITICAL: Failed to move event {} to DLQ! Original error: {}, DLQ error: {}",
                event.getEventId(), exception.getMessage(), dlqException.getMessage(), dlqException);
        }
    }

}
