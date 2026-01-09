package com.pm.inventoryservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckResponseDTO {
    private UUID productId;
    private boolean available;
    private Integer quantityAvailable;
    private Integer quantityReserved;
}
