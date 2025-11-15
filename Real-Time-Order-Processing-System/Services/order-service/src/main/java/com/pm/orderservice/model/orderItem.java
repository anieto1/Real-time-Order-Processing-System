package com.pm.orderservice.model;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;
import java.util.*;


@Entity
@Table(name = "orderItems")
@Data
public class orderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Size(min = 0)
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", nullable = false)
    private double price;
}
