package com.pm.inventoryservice.dto.response;

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
public class InventoryResponseDTO {
    private UUID inventoryId;
    private UUID productId;
    private String productName;
    private String sku;
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private Integer reorderLevel;
    private String warehouseLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getTotalQuantity() {
        if (quantityAvailable == null && quantityReserved == null) {
            return null;
        }
        return (quantityAvailable != null ? quantityAvailable : 0) +
                (quantityReserved != null ? quantityReserved : 0);
    }

    public Boolean getIsLowStock() {
        if (quantityAvailable == null || reorderLevel == null) {
            return null;
        }
        return quantityAvailable <= reorderLevel;
    }
}
