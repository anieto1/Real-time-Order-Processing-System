package com.pm.inventoryservice.dto.response;

import com.pm.inventoryservice.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResponseDTO {

    private UUID reservationId;
    private UUID orderId;
    private UUID productId;
    private Integer quantityReserved;
    private ReservationStatus reservationStatus;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
