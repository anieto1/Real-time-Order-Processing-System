package com.pm.orderservice.controller;

import com.pm.orderservice.model.DeadLetterEvent;
import com.pm.orderservice.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // TODO: Add reprocessing endpoints later
    // - POST /{dlqId}/reprocess
    // - POST /reprocess-all
}
