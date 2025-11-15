package com.pm.orderservice.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.*;
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "outboxEvents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType = "ORDER";

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @CreatedDate
    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "publishedAt", nullable = true)
    private LocalDateTime publishedAt;
}
