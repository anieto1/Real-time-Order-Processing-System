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
public class InventoryEventDTO {

    private UUID eventId;
    private EventType eventType;
    private UUID orderId;
    private UUID productId;
    private Integer quantity;
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
}
