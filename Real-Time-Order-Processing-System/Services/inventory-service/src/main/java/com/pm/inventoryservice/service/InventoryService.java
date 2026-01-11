package com.pm.inventoryservice.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.inventoryservice.dto.request.InventoryCreateRequestDTO;
import com.pm.inventoryservice.dto.request.InventoryUpdateRequestDTO;
import com.pm.inventoryservice.dto.response.InventoryResponseDTO;
import com.pm.inventoryservice.exception.InventoryNotFoundException;
import com.pm.inventoryservice.mapper.InventoryMapper;
import com.pm.inventoryservice.model.EventType;
import com.pm.inventoryservice.model.Inventory;
import com.pm.inventoryservice.model.OutboxEvent;
import com.pm.inventoryservice.repository.InventoryRepository;
import com.pm.inventoryservice.repository.OutboxEventRepository;
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

    //TODO:update not fully implemented yet
    @Transactional
    public InventoryResponseDTO updateInventory(UUID productId, InventoryUpdateRequestDTO updateRequestDTO){
        if(!inventoryRepository.existsByProductId(productId)){
            throw new InventoryNotFoundException(productId.toString());
        }
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(()-> new InventoryNotFoundException(productId.toString()));
        return inventoryMapper.toResponseDTO(inventory);
    }



//    @Transactional
//    public InventoryResponseDTO deleteInventory(UUID productId){
//        Inventory inventory = inventoryRepository.findByProductId(productId)
//                .orElseThrow(()-> new InventoryNotFoundException(productId.toString()));
//
//       // if(inventory.){}
//    }

}
