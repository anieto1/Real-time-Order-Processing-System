package com.pm.orderservice.model;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;
import java.util.*;


@Entity
@Table(name = "orders")
@Data
public class order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column
    private status orderStatus = status.PENDING;

    @Size(min = 0)
    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @CreatedDate
    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;


    @Column(name = "version", nullable = false)
    private int version;

}
