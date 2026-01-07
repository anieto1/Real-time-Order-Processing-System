package com.pm.inventoryservice.repository;

import com.pm.inventoryservice.model.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {
    List<DeadLetterEvent> findByResolvedFalseOrderByMovedToDlqAtDesc();
    long countByResolvedFalse();
    List<DeadLetterEvent>findByMovedToDlqAtBetween(LocalDateTime start, LocalDateTime end);
    Optional<DeadLetterEvent> findByOriginalEventId(UUID originalEventId);


}
