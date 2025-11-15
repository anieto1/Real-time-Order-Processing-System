package com.pm.orderservice.repository;
import com.pm.orderservice.model.outboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface outboxEventRepository extends JpaRepository<outboxEvent, UUID>{
}
