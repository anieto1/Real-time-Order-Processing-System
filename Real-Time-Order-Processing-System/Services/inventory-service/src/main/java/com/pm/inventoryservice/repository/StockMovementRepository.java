package com.pm.inventoryservice.repository;

import com.pm.inventoryservice.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByInventoryIdOrderByCreatedAtDesc(UUID inventoryId);

    List<StockMovement> findByReferenceIdAndReferenceType(UUID refId, String refType);

    List<StockMovement> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
