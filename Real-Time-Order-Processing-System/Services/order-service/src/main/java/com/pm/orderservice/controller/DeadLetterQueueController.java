package com.pm.orderservice.controller;

import com.pm.orderservice.model.DeadLetterEvent;
import com.pm.orderservice.model.OutboxEvent;
import com.pm.orderservice.repository.DeadLetterEventRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueController {

    private final DeadLetterEventRepository deadLetterEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    @GetMapping("/unresolved")
    public ResponseEntity<List<DeadLetterEvent>> getUnresolvedEvents() {
        List<DeadLetterEvent> events = deadLetterEventRepository
            .findByResolvedFalseOrderByMovedToDlqAtDesc();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalCount = deadLetterEventRepository.count();
        long unresolvedCount = deadLetterEventRepository
            .findByResolvedFalseOrderByMovedToDlqAtDesc().size();

        stats.put("totalCount", totalCount);
        stats.put("unresolvedCount", unresolvedCount);
        stats.put("resolvedCount", totalCount - unresolvedCount);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{dlqId}")
    public ResponseEntity<DeadLetterEvent> getDlqEvent(@PathVariable UUID dlqId) {
        return deadLetterEventRepository.findById(dlqId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping("/{dlqId}/reprocess")
    public ResponseEntity<String> reprocessEvent(@PathVariable UUID dlqId, @RequestParam String resolvedBy) {

        DeadLetterEvent dlqEvent = deadLetterEventRepository.findById(dlqId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DLQ event not found"));

        if (dlqEvent.isResolved()) {
            log.info("Event {} already resolved", dlqId);
            return ResponseEntity.badRequest().build();
        }

        OutboxEvent originalEvent = outboxEventRepository.findById(dlqEvent.getOriginalEventId())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Original event not found"));

        originalEvent.setPublished(false);
        originalEvent.setRetryCount(0);
        originalEvent.setPublishedAt(null);
        outboxEventRepository.save(originalEvent);

        dlqEvent.setResolved(true);

        dlqEvent.setResolvedBy(resolvedBy);
        dlqEvent.setResolvedAt(java.time.LocalDateTime.now());
        deadLetterEventRepository.save(dlqEvent);

        return ResponseEntity.ok("Event queued for reprocessing");

    }

    //@PostMapping
    //Reprocess-all method

    }

