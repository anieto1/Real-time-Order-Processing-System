package com.pm.inventoryservice.dto.response;

import com.pm.inventoryservice.model.MovementType;
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
public class StockMovementResponseDTO {

    private UUID movementId;
    private MovementType movementType;
    private Integer quantity;
    private Integer previousQuantity;
    private Integer newQuantity;
    private UUID referenceId;
    private String referenceType;
    private String reason;
    private LocalDateTime createdAt;


}
