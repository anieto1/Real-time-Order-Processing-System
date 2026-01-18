package com.pm.inventoryservice.service;

import com.pm.inventoryservice.dto.eventDTO.DLQStatsDTO;
import com.pm.inventoryservice.dto.eventDTO.DeadLetterEventDTO;
import com.pm.inventoryservice.exception.NotFoundException;
import com.pm.inventoryservice.mapper.DeadLetterEventMapper;
import com.pm.inventoryservice.model.DLQStatus;
import com.pm.inventoryservice.model.DeadLetterEvent;
import com.pm.inventoryservice.repository.DeadLetterEventRepository;
import com.pm.inventoryservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final DeadLetterEventRepository deadLetterEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final DeadLetterEventMapper deadLetterEventMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional(readOnly = true)
    public List<DeadLetterEventDTO> getUnresolvedEvents(){
        List<DeadLetterEvent> events = deadLetterEventRepository.findByResolvedFalseOrderByMovedToDlqAtDesc();

        if(events.isEmpty()){
            log.info("No unresolved events found in DLQ");
            return List.of();
        }

        Collections.reverse(events);
        return events.stream()
                .map(deadLetterEventMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DLQStatsDTO getStats() {
        long totalUnresolved = deadLetterEventRepository.countByResolvedFalse();
        long totalResolved = deadLetterEventRepository.count() - totalUnresolved;

        List<DeadLetterEvent> unresolvedEvents = deadLetterEventRepository.findByResolvedFalseOrderByMovedToDlqAtDesc();

        Duration oldestAge;
        Map<String, Long> failureReasonBreakdown;

        if (unresolvedEvents.isEmpty()) {
            oldestAge = Duration.ZERO;
            failureReasonBreakdown = Collections.emptyMap();
        } else {
            DeadLetterEvent oldestEvent = unresolvedEvents.getLast();
            oldestAge = Duration.between(oldestEvent.getMovedToDlqAt(), java.time.LocalDateTime.now());

            failureReasonBreakdown = unresolvedEvents.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            DeadLetterEvent::getFailureReason,
                            java.util.stream.Collectors.counting()
                    ));
        }

        return new DLQStatsDTO(totalUnresolved, totalResolved, oldestAge, failureReasonBreakdown);
    }

    @Transactional
    public boolean reprocessEvent(UUID dlqId){
        DeadLetterEvent event = deadLetterEventRepository.findById(dlqId)
                .orElseThrow(() -> new NotFoundException("DLQ event not found"));

        if(event.isResolved()){
            throw new IllegalStateException("Event already resolved, cannot reprocess");
        }
        event.setStatus(DLQStatus.REPROCESSING);
        deadLetterEventRepository.save(event);

        try {
            kafkaTemplate.send("inventory-events", event.getAggregateId().toString(), event.getPayload()).get();
            event.setResolved(true);
            event.setStatus(DLQStatus.RESOLVED);
            event.setResolvedAt(LocalDateTime.now());
            event.setResolvedBy("SYSTEM");
            deadLetterEventRepository.save(event);

            outboxEventRepository.findById(event.getOriginalEventId())
                    .ifPresent(outbox -> {
                        outbox.setPublished(true);
                        outbox.setPublishedAt(LocalDateTime.now());
                        outboxEventRepository.save(outbox);
                    });
            log.info("Successfully reprocessed DLQ event {}", dlqId);
            return true;
        } catch (Exception e) {
            log.error("Failed to reprocess event {}", dlqId, e);
            event.setStatus(DLQStatus.UNRESOLVED);
            deadLetterEventRepository.save(event);
            return false;
        }


    }

    @Transactional
    public void markAsResolved(UUID dlqId, String resolvedBy){
        if(resolvedBy == null || resolvedBy.isBlank()){
            throw new IllegalArgumentException("ResolvedBy cannot be null or blank");
        }

        DeadLetterEvent event = deadLetterEventRepository.findById(dlqId)
                .orElseThrow(() -> new NotFoundException("DLQ event not found"));
        if(event.isResolved()){
            throw new IllegalStateException("Event already resolved");
        }
        event.setResolved(true);
        event.setResolvedAt(LocalDateTime.now());
        event.setResolvedBy(resolvedBy);
        deadLetterEventRepository.save(event);

        log.info("DLQ event {} marked as resolved by {}", dlqId, resolvedBy);
    }



}
