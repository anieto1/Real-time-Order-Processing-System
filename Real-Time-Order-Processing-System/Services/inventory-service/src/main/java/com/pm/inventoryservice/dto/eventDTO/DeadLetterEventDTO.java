package com.pm.inventoryservice.dto.eventDTO;

import com.pm.inventoryservice.model.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEventDTO {
    private UUID dlqId;
    private UUID originalEventId;
    private UUID aggregateId;
    private EventType eventType;
    private String payload;
    private int retryCount;
    private String failureReason;
    private LocalDateTime movedToDlqAt;
    private boolean resolved;
    private LocalDateTime resolvedAt;
    private String resolvedBy;

}
