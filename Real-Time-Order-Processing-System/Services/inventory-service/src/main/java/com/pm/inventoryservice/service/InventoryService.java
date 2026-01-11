package com.pm.inventoryservice.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.inventoryservice.dto.request.InventoryCreateRequestDTO;
import com.pm.inventoryservice.dto.request.InventoryUpdateRequestDTO;
import com.pm.inventoryservice.dto.response.InventoryResponseDTO;
import com.pm.inventoryservice.exception.InventoryNotFoundException;
import com.pm.inventoryservice.exception.StockOperationException;
import com.pm.inventoryservice.mapper.InventoryMapper;
import com.pm.inventoryservice.model.EventType;
import com.pm.inventoryservice.model.Inventory;
import com.pm.inventoryservice.model.OutboxEvent;
import com.pm.inventoryservice.repository.InventoryRepository;
import com.pm.inventoryservice.repository.OutboxEventRepository;
import com.pm.inventoryservice.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StockReservationRepository stockReservationRepository;
    private final InventoryMapper inventoryMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public InventoryResponseDTO createInventory(InventoryCreateRequestDTO createRequestDTO, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(createRequestDTO.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(createRequestDTO.getProductId().toString()));

        Inventory newInventory = inventoryMapper.toEntity(createRequestDTO);
        newInventory.setQuantityAvailable(quantity);
        inventoryRepository.save(newInventory);
        
        
        
        String payload;
        try{
            InventoryResponseDTO responseDTO = inventoryMapper.toResponseDTO(newInventory);
            payload = objectMapper.writeValueAsString(responseDTO);
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing inventory response: {}", e.getMessage());
            throw new RuntimeException("Error serializing inventory response:");
        }
        
        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(newInventory.getInventoryId())
                .aggregateType("INVENTORY")
                .eventType(EventType.STOCK_RESTOCKED)
                .payload(payload)
                .published(false)
                .build();
        outboxEventRepository.save(event);
        
        
        
        return inventoryMapper.toResponseDTO(newInventory);
    }
    
    @Transactional(readOnly = true)
    public InventoryResponseDTO getInventoryByProductId(UUID productId){
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(()-> new InventoryNotFoundException(productId.toString()));
        
        return inventoryMapper.toResponseDTO(inventory);
    }
    
    @Transactional(readOnly = true)
    public InventoryResponseDTO getInventoryBySku(String sku){
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(()-> new InventoryNotFoundException(sku));
        return inventoryMapper.toResponseDTO(inventory);
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponseDTO> getAllInventory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inventory> inventoryPage = inventoryRepository.findAll(pageable);
        return inventoryPage.map(inventoryMapper::toResponseDTO);
    }

    @Transactional
    public InventoryResponseDTO updateInventory(UUID productId, InventoryUpdateRequestDTO updateRequestDTO) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId.toString()));

        if (isEmptyUpdate(updateRequestDTO)) {
            throw new StockOperationException("Update request must contain at least one field to update");
        }

        if (updateRequestDTO.getProductName() != null) {
            if (updateRequestDTO.getProductName().isBlank()) {
                throw new StockOperationException("Product name cannot be blank");
            }
            inventory.setProductName(updateRequestDTO.getProductName());
        }

        if (updateRequestDTO.getReorderLevel() != null) {
            if (updateRequestDTO.getReorderLevel() < 0) {
                throw new StockOperationException("Reorder level must be >= 0");
            }
            inventory.setReorderLevel(updateRequestDTO.getReorderLevel());
        }

        if (updateRequestDTO.getReorderQuantity() != null) {
            if (updateRequestDTO.getReorderQuantity() < 1) {
                throw new StockOperationException("Reorder quantity must be >= 1");
            }
            inventory.setReorderQuantity(updateRequestDTO.getReorderQuantity());
        }

        if (updateRequestDTO.getWarehouseLocation() != null) {
            inventory.setWarehouseLocation(updateRequestDTO.getWarehouseLocation());
        }

        Inventory updatedInventory = inventoryRepository.save(inventory);
        if (updateRequestDTO.getReorderLevel() != null) {
            checkAndPublishLowStockAlert(updatedInventory);
        }

        log.info("Updated inventory for productId: {}", productId);
        return inventoryMapper.toResponseDTO(updatedInventory);
    }




    @Transactional
    public InventoryResponseDTO deleteInventory(UUID productId){
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(()-> new InventoryNotFoundException(productId.toString()));
    }


    private void checkAndPublishLowStockAlert(Inventory inventory) {
        if (inventory.getQuantityAvailable() <= inventory.getReorderLevel()) {
            log.warn("Low stock detected for productId: {}, available: {}, reorderLevel: {}",
                    inventory.getProductId(),
                    inventory.getQuantityAvailable(),
                    inventory.getReorderLevel());

            try {
                InventoryResponseDTO responseDTO = inventoryMapper.toResponseDTO(inventory);
                String payload = objectMapper.writeValueAsString(responseDTO);

                OutboxEvent event = OutboxEvent.builder()
                        .aggregateId(inventory.getInventoryId())
                        .aggregateType("INVENTORY")
                        .eventType(EventType.LOW_STOCK_ALERT)
                        .payload(payload)
                        .published(false)
                        .build();
                outboxEventRepository.save(event);

            } catch (JsonProcessingException e) {
                log.error("Error serializing low stock alert: {}", e.getMessage());
            }
        }
    }
    private boolean isEmptyUpdate(InventoryUpdateRequestDTO updateRequestDTO) {
        return updateRequestDTO.getProductName() == null
                && updateRequestDTO.getReorderLevel() == null
                && updateRequestDTO.getReorderQuantity() == null
                && updateRequestDTO.getWarehouseLocation() == null;
    }

}
