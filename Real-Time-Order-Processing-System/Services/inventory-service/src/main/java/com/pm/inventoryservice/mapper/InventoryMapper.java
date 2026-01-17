package com.pm.inventoryservice.mapper;

import  com.pm.inventoryservice.dto.request.InventoryCreateRequestDTO;
import com.pm.inventoryservice.dto.response.InventoryResponseDTO;
import com.pm.inventoryservice.model.Inventory;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InventoryMapper {

    // productName must come from external source (e.g., product service) - ignore here
    @Mapping(target = "productName", ignore = true)
    InventoryResponseDTO toResponseDTO(Inventory inventory);

    @Mapping(target = "inventoryId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "quantityReserved", constant = "0")
    @Mapping(source = "initialQuantity", target = "quantityAvailable")
    Inventory toEntity(InventoryCreateRequestDTO dto);
}
