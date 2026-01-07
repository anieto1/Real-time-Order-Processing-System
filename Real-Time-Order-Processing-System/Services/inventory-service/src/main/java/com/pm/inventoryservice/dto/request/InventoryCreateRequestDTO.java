package com.pm.inventoryservice.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCreateRequestDTO {
    
    @NotNull(message = "Product ID is required")
    private UUID productId;
    
    @NotBlank(message = "Prodcut name is required")
    private String productName;
    
    @NotBlank(message = "Sku is requried")
    private String sku;
    
    @Min(0)
    private Integer initialQuantity;

    @Min(0)
    private Integer reorderLevel;

    @Min(1)
    private Integer reorderQuantity;

    private String warehouseLocation;

}
