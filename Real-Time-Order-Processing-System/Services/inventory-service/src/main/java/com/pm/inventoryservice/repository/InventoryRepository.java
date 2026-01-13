package com.pm.inventoryservice.repository;

import com.pm.inventoryservice.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByProductId(UUID productId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findByProductIdForUpdate(UUID productId);
    Optional<Inventory> findBySku(String sku);
    boolean existsByProductId(UUID productId);
    boolean existsBySku(String sku);
    List<Inventory> findByQuantityAvailableLessThanEqual(Integer quantity);


    
    
}
