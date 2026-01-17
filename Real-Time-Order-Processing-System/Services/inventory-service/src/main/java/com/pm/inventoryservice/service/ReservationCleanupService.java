package com.pm.inventoryservice.service;

import com.pm.inventoryservice.model.StockReservation;
import com.pm.inventoryservice.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.pm.inventoryservice.model.ReservationStatus;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService  {

    private final StockReservationRepository stockReservationRepository;
    private final InventoryService inventoryService;
    @Scheduled(fixedRate = 500000)
    public void cleanUpExpiredReservations() {

        List<StockReservation> stockReservations = stockReservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, java.time.LocalDateTime.now());

        if(stockReservations.isEmpty()){
            log.info("No expired reservations found");
            return;
        }

        List<UUID> uniqueOrderIds = stockReservations.stream()
                .map(StockReservation::getOrderId)
                .distinct()
                .toList();

        int successCount = 0;
        int failureCount = 0;

        for(UUID orderId : uniqueOrderIds) {
            try {
                inventoryService.releaseReservation(orderId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to release reservation for orderId: {}, error: {}",
                        orderId, e.getMessage());
            }
        }

        log.info("Cleanup complete: {} orders released, {} failures", successCount, failureCount);
    }

}
