package com.pm.orderservice.model;
import lombok.Data;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.*;
@Entity
@Table(name = "outboxEvents")
@Data
public class outboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @ManyToOne
    @JoinColumn(name = "aggregate_id", nullable = false)
    private order aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType = "ORDER";

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private eventType eventType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @CreatedDate
    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @CreatedDate
    @Column(name = "publishedAt", nullable = false)
    private LocalDateTime publishedAt;
}
