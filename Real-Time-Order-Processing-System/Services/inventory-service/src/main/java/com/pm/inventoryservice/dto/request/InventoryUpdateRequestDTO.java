package com.pm.inventoryservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequestDTO {
    private String productName;
    private Integer reorderLevel;
    private Integer reorderQuantity;
    private String warehouseLocation;
}
