package com.pm.inventoryservice.controller;

import com.pm.inventoryservice.dto.eventDTO.DLQStatsDTO;
import com.pm.inventoryservice.dto.eventDTO.DeadLetterEventDTO;
import com.pm.inventoryservice.service.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueController {

    private final DeadLetterQueueService deadLetterQueueService;

    @GetMapping("/unresolved")
    public ResponseEntity<List<DeadLetterEventDTO>> getUnresolvedEvents() {
        log.info("Fetching unresolved DLQ events");
        List<DeadLetterEventDTO> events = deadLetterQueueService.getUnresolvedEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/stats")
    public ResponseEntity<DLQStatsDTO> getStats() {
        log.info("Fetching DLQ statistics");
        DLQStatsDTO stats = deadLetterQueueService.getStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{dlqId}/reprocess")
    public ResponseEntity<String> reprocessEvent(@PathVariable UUID dlqId) {
        log.info("Reprocessing DLQ event: {}", dlqId);
        boolean success = deadLetterQueueService.reprocessEvent(dlqId);
        if (success) {
            return ResponseEntity.ok("Event reprocessed successfully");
        } else {
            return ResponseEntity.status(500).body("Failed to reprocess event");
        }
    }

    @PostMapping("/{dlqId}/resolve")
    public ResponseEntity<String> markAsResolved(@PathVariable UUID dlqId, @RequestParam String resolvedBy) {
        log.info("Marking DLQ event {} as resolved by {}", dlqId, resolvedBy);
        deadLetterQueueService.markAsResolved(dlqId, resolvedBy);
        return ResponseEntity.ok("Event marked as resolved");
    }
}
