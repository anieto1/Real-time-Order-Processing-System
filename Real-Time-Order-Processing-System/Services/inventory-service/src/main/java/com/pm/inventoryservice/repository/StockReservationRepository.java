package com.pm.inventoryservice.repository;

import com.pm.inventoryservice.model.StockReservation;
import com.pm.inventoryservice.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {
    
    StockReservation findByOrderId(UUID orderId);

    StockReservation findByOrderIdAndProductId(UUID orderId, UUID productId);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime time);

    List<StockReservation> findByProductIdAndStatus(UUID productId, ReservationStatus status);

}
