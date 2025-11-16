package com.pm.orderservice.dto;

import com.pm.orderservice.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private UUID orderId;

    private UUID customerId;

    private Status orderStatus;

    private BigDecimal totalAmount;

    private List<OrderItemResponseDTO> items;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
