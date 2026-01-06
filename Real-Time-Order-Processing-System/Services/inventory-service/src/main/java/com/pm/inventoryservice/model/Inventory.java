package com.pm.inventoryservice.model;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_id", nullable = false)
    private UUID inventoryId;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "sku", nullable = false)
    private String sku;
    
    @Column(name = "quantityAvailable", nullable = false)
    @Size(min = 0)
    private int quantityAvailable;
    
    @Column(name = "quantityReserved", nullable = false)
    @Size(min = 0)
    private int quantityReserved;
    
    @Column(name = "reorderLevel", nullable = false)
    private int reorderLevel;

    @Column(name = "reorderQuantity", nullable = false)
    private int reorderQuantity;

    @Column(name = "warehouseLocation")
    private String warehouseLocation;

    @CreatedDate
    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
