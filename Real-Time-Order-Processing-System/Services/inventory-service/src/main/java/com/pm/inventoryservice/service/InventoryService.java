package com.pm.inventoryservice.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.inventoryservice.dto.request.*;
import com.pm.inventoryservice.dto.response.InventoryResponseDTO;
import com.pm.inventoryservice.dto.response.StockCheckResponseDTO;
import com.pm.inventoryservice.dto.response.StockReservationResponseDTO;
import com.pm.inventoryservice.exception.InvalidReservationStateException;
import com.pm.inventoryservice.exception.InventoryNotFoundException;
import com.pm.inventoryservice.exception.StockOperationException;
import com.pm.inventoryservice.mapper.InventoryMapper;
import com.pm.inventoryservice.model.*;
import com.pm.inventoryservice.repository.InventoryRepository;
import com.pm.inventoryservice.repository.OutboxEventRepository;
import com.pm.inventoryservice.repository.StockMovementRepository;
import com.pm.inventoryservice.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper inventoryMapper;
    private final ObjectMapper objectMapper;

    private static final int RESERVATION_EXPIRY_MINUTES = 15;

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
        Inventory inventory = getInventoryOrThrow(productId);
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
        Inventory inventory = getInventoryOrThrow(productId);

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
        Inventory inventory = getInventoryOrThrow(productId);

        if(!stockReservationRepository.findByProductIdAndStatus(productId, ReservationStatus.PENDING).isEmpty()){
            throw new StockOperationException("Cannot delete inventory with pending reservation");
        }

        if(inventory.getQuantityReserved() > 0){
            throw new StockOperationException("Cannot delete inventory with reserved quantity");
        }

        if(inventory.getQuantityAvailable() > 0){
            throw new StockOperationException("Cannot delete inventory with available quantity");
        }

        outboxEventRepository.deleteByAggregateIdAndPublishedFalse(inventory.getInventoryId());
        inventory.setDeletedAt(LocalDateTime.now());
        inventoryRepository.save(inventory);
        log.info("Deleted inventory for productId: {}", productId);
        return inventoryMapper.toResponseDTO(inventory);
    }




//STOCK OPERATIONS

    @Transactional(readOnly = true)
    public StockCheckResponseDTO checkStock(UUID productId, int quantity) {
            Inventory inventory = getInventoryOrThrow(productId);
            return StockCheckResponseDTO.builder()
                    .productId(productId)
                    .available(inventory.getQuantityAvailable() >= quantity)
                    .quantityAvailable(inventory.getQuantityAvailable())
                    .quantityRequested(quantity)
                    .quantityReserved(inventory.getQuantityReserved())
                    .build();
        }

    @Transactional(readOnly = true)
    public List<StockCheckResponseDTO> checkStockBatch(List<ReservationItemDTO> items) {
        List<StockCheckResponseDTO> results = new ArrayList<>();

        for (ReservationItemDTO item : items) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).orElse(null);

            if (inventory == null) {
                results.add(StockCheckResponseDTO.builder()
                        .productId(item.getProductId())
                        .available(false)
                        .quantityAvailable(0)
                        .quantityRequested(item.getQuantity())
                        .quantityReserved(0)
                        .build());
            } else {
                results.add(StockCheckResponseDTO.builder()
                        .productId(item.getProductId())
                        .available(inventory.getQuantityAvailable() >= item.getQuantity())
                        .quantityAvailable(inventory.getQuantityAvailable())
                        .quantityRequested(item.getQuantity())
                        .quantityReserved(inventory.getQuantityReserved())
                        .build());
            }
        }

        return results;
    }


    @Transactional
    public InventoryResponseDTO addStock(UUID productId, int quantity, String reason) {
        Inventory inventory = getInventoryOrThrow(productId);
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);
        inventoryRepository.save(inventory);
        log.info("Added {} to inventory for productId: {}, reason: {}", quantity, productId, reason);
        return inventoryMapper.toResponseDTO(inventory);
    }


    @Transactional
    public InventoryResponseDTO adjustStock(UUID productId, StockAdjustmentRequestDTO adjustmentRequestDTO){
        Inventory inventory = getInventoryOrThrow(productId);
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + adjustmentRequestDTO.getQuantity());
        inventoryRepository.save(inventory);
        log.info("Adjusted inventory for productId: {}, reason: {}", productId, adjustmentRequestDTO.getReason());
        return inventoryMapper.toResponseDTO(inventory);
    }


//RESERVATION OPERATIONS
    @Transactional
    public List<StockReservationResponseDTO> reserveStock(UUID orderId, List<ReservationItemDTO> items) {
        List<StockReservation> existingReservations = stockReservationRepository.findByOrderId(orderId);
        if (!existingReservations.isEmpty()) {
            log.info("Reservations already exist for orderId: {}, returning existing", orderId);
            return existingReservations.stream()
                    .map(this::toReservationResponseDTO)
                    .toList();
        }

        for (ReservationItemDTO item : items) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException(item.getProductId().toString()));

            if (inventory.getQuantityAvailable() < item.getQuantity()) {
                log.warn("Insufficient stock for productId: {}, available: {}, requested: {}",
                        item.getProductId(), inventory.getQuantityAvailable(), item.getQuantity());
                throw new StockOperationException("Insufficient stock for productId: " + item.getProductId());
            }
        }

        List<StockReservationResponseDTO> results = new ArrayList<>();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESERVATION_EXPIRY_MINUTES);

        for (ReservationItemDTO item : items) {
            Inventory inventory = inventoryRepository.findByProductIdWithLock(item.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException(item.getProductId().toString()));

            int previousQuantity = inventory.getQuantityAvailable();

            inventory.setQuantityAvailable(inventory.getQuantityAvailable() - item.getQuantity());
            inventory.setQuantityReserved(inventory.getQuantityReserved() + item.getQuantity());
            inventoryRepository.save(inventory);

            StockReservation reservation = StockReservation.builder()
                    .orderId(orderId)
                    .productId(item.getProductId())
                    .quantityReserved(item.getQuantity())
                    .status(ReservationStatus.PENDING)
                    .expiresAt(expiresAt)
                    .build();
            StockReservation savedReservation = stockReservationRepository.save(reservation);

            StockMovement movement = StockMovement.builder()
                    .inventoryId(inventory.getInventoryId())
                    .movementType(MovementType.RESERVATION)
                    .quantity(item.getQuantity())
                    .previousQuantity(previousQuantity)
                    .newQuantity(inventory.getQuantityAvailable())
                    .referenceId(orderId)
                    .referenceType("ORDER")
                    .reason("Stock reserved for order")
                    .build();
            stockMovementRepository.save(movement);
            checkAndPublishLowStockAlert(inventory);

            results.add(toReservationResponseDTO(savedReservation));
        }
        publishStockReservedEvent(orderId, items);

        log.info("Reserved stock for orderId: {}, items: {}", orderId, items.size());
        return results;
    }

    @Transactional
    public List<StockReservationResponseDTO> confirmReservation(UUID orderId){
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        if(reservations.isEmpty()){
            throw new StockOperationException("No reservations found for orderId: " + orderId);
        }

        for(StockReservation reservation : reservations){
            if(reservation.getStatus() == ReservationStatus.CONFIRMED){
                log.info("Reservation already confirmed for orderId: {}, returning existing", orderId);
                return reservations.stream()
                        .map(this::toReservationResponseDTO)
                        .toList();
            }

            if(reservation.getStatus() == ReservationStatus.RELEASED || reservation.getStatus() == ReservationStatus.EXPIRED){
                throw new InvalidReservationStateException("Reservation already released or expired for orderId: " + orderId);
            }

            if (reservation.getExpiresAt() != null && reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new InvalidReservationStateException("Reservation has expired for orderId: " + orderId);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        for(StockReservation reservation : reservations){
            Inventory inventory = getInventoryOrThrow(reservation.getProductId());
            int previousReserved = inventory.getQuantityReserved();
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantityReserved());
            inventoryRepository.save(inventory);

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setConfirmedAt(now);
            stockReservationRepository.save(reservation);

            StockMovement movement = StockMovement.builder()
                    .inventoryId(inventory.getInventoryId())
                    .movementType(MovementType.RESERVATION_CONFIRMED)
                    .quantity(reservation.getQuantityReserved())
                    .previousQuantity(previousReserved)
                    .newQuantity(inventory.getQuantityReserved())
                    .referenceId(orderId)
                    .referenceType("ORDER")
                    .reason("Order confirmed")
                    .createdBy("SYSTEM")
                    .build();
            stockMovementRepository.save(movement);
        }

        try{
            String payload = objectMapper.writeValueAsString(reservations.stream()
                    .map(this::toReservationResponseDTO)
                    .toList());

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(orderId)
                    .aggregateType("ORDER")
                    .eventType(EventType.RESERVATION_CONFIRMED)
                    .payload(payload)
                    .published(false)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e){
            log.error("Error serializing stock reservation response: {}", e.getMessage());
            throw new RuntimeException("Error serializing stock reservation response");
        }

        log.info("Confirmed reservations for orderId: {}", orderId);
        return reservations.stream()
                .map(this::toReservationResponseDTO)
                .toList();
    }

    @Transactional
    public void releaseReservation(UUID orderId){
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        if(reservations.isEmpty()){
            throw new StockOperationException("No reservations found for orderId: " + orderId);
        }
        for(StockReservation reservation : reservations){
            if(reservation.getStatus() == ReservationStatus.RELEASED){
                log.info("Reservation already released for orderId: {}, returning existing", orderId);
                return;
            }

            if(reservation.getStatus() == ReservationStatus.CONFIRMED){
                throw new InvalidReservationStateException("Reservation already confirmed for orderId: " + orderId);
            }
        }




        for(StockReservation reservation : reservations){
            Inventory inventory = getInventoryOrThrow(reservation.getProductId());
            int previousAvailable = inventory.getQuantityAvailable();
            int previousReserved = inventory.getQuantityReserved();

            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + reservation.getQuantityReserved());
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantityReserved());
            inventoryRepository.save(inventory);

            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReleasedAt(LocalDateTime.now());
            stockReservationRepository.save(reservation);

            StockMovement movement = StockMovement.builder()
                    .inventoryId(inventory.getInventoryId())
                    .movementType(MovementType.RESERVATION_RELEASED)
                    .quantity(reservation.getQuantityReserved())
                    .previousQuantity(previousAvailable)
                    .newQuantity(inventory.getQuantityAvailable())
                    .referenceId(orderId)
                    .referenceType("ORDER")
                    .reason("Reservation released")
                    .createdBy("SYSTEM")
                    .build();
            stockMovementRepository.save(movement);


        }
        try{
            String payload = objectMapper.writeValueAsString(reservations.stream()
                    .map(this::toReservationResponseDTO)
                    .toList());

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(orderId)
                    .aggregateType("ORDER")
                    .eventType(EventType.RESERVATION_RELEASED)
                    .payload(payload)
                    .published(false)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e){
            log.error("Error serializing reservation release: {}", e.getMessage());
            throw new RuntimeException("Error serializing reservation release");
        }

        log.info("Released reservations for orderId: {}", orderId);


    }




    //HELPER METHODS
    private Inventory getInventoryOrThrow(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId.toString()));
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
    private StockReservationResponseDTO toReservationResponseDTO(StockReservation reservation) {
        return StockReservationResponseDTO.builder()
                .reservationId(reservation.getReservationId())
                .orderId(reservation.getOrderId())
                .productId(reservation.getProductId())
                .quantityReserved(reservation.getQuantityReserved())
                .reservationStatus(reservation.getStatus())
                .expiresAt(reservation.getExpiresAt())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    private void publishStockReservedEvent(UUID orderId, List<ReservationItemDTO> items) {
        try {
            String payload = objectMapper.writeValueAsString(items);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(orderId)
                    .aggregateType("INVENTORY")
                    .eventType(EventType.STOCK_RESERVED)
                    .payload(payload)
                    .published(false)
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Error serializing stock reserved event: {}", e.getMessage());
        }
    }

}
