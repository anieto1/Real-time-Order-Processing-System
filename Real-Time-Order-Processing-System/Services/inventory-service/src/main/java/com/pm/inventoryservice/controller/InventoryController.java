package com.pm.inventoryservice.controller;


import com.pm.inventoryservice.dto.request.InventoryCreateRequestDTO;
import com.pm.inventoryservice.dto.request.InventoryUpdateRequestDTO;
import com.pm.inventoryservice.dto.request.ReservationItemDTO;
import com.pm.inventoryservice.dto.request.StockAdjustmentRequestDTO;
import com.pm.inventoryservice.dto.response.InventoryResponseDTO;
import com.pm.inventoryservice.dto.response.StockCheckResponseDTO;
import com.pm.inventoryservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/inventory")
@Tag(name = "Inventory Management", description = "Inventory Management API")
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    @PostMapping
    @Operation(summary = "Create inventory", description = "Creates a new inventory item and adds available stock")
    public ResponseEntity<InventoryResponseDTO> createInventory(@Valid @RequestBody InventoryCreateRequestDTO requestDTO){
        return ResponseEntity.ok(inventoryService.createInventory(requestDTO, requestDTO.getInitialQuantity()));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory by product ID", description = "Retrieves inventory information for a specific product")
    public ResponseEntity<InventoryResponseDTO> getInventoryByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get inventory by SKU", description = "Retrieves inventory information by SKU")
    public ResponseEntity<InventoryResponseDTO> getInventoryBySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getInventoryBySku(sku));
    }

    @GetMapping
    @Operation(summary = "Get all inventory", description = "Retrieves all inventory items with pagination")
    public ResponseEntity<?> getAllInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getAllInventory(page, size));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update inventory", description = "Updates inventory information for a product")
    public ResponseEntity<InventoryResponseDTO> updateInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryUpdateRequestDTO requestDTO) {
        return ResponseEntity.ok(inventoryService.updateInventory(productId, requestDTO));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete inventory", description = "Deletes inventory for a product")
    public ResponseEntity<Void> deleteInventory(@PathVariable UUID productId) {
        inventoryService.deleteInventory(productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/stock")
    @Operation(summary = "Add stock", description = "Adds stock quantity to a product")
    public ResponseEntity<InventoryResponseDTO> addStock(
            @PathVariable UUID productId,
            @RequestParam int quantity,
            @RequestParam String reason) {
        return ResponseEntity.ok(inventoryService.addStock(productId, quantity, reason));
    }

    @PutMapping("/{productId}/stock")
    @Operation(summary = "Adjust stock", description = "Adjusts stock quantity for a product")
    public ResponseEntity<InventoryResponseDTO> adjustStock(
            @PathVariable UUID productId,
            @RequestParam StockAdjustmentRequestDTO adjustment) {
        return ResponseEntity.ok(inventoryService.adjustStock(productId, adjustment));
    }

    @GetMapping("/{productId}/stock")
    @Operation(summary = "Check stock", description = "Checks current stock level for a product")
    public ResponseEntity<?> checkStock(@PathVariable UUID productId, @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.checkStock(productId, quantity));
    }

    @PostMapping("/stock/check-batch")
    @Operation(summary = "Check stock batch", description = "Checks stock levels for multiple products")
    public ResponseEntity<?> checkStockBatch(@Valid @RequestBody List<ReservationItemDTO> items) {
        return ResponseEntity.ok(inventoryService.checkStockBatch(items));
    }

    @PostMapping("/reservations")
    @Operation(summary = "Reserve stock", description = "Reserves stock for an order")
    public ResponseEntity<?> reserveStock(@RequestParam UUID orderId, @RequestBody List<ReservationItemDTO> items) {
        return ResponseEntity.ok(inventoryService.reserveStock(orderId, items));
    }

    @PutMapping("/reservations/{orderId}/confirm")
    @Operation(summary = "Confirm reservation", description = "Confirms a stock reservation")
    public ResponseEntity<?> confirmReservation(@PathVariable UUID orderId) {
        return ResponseEntity.ok(inventoryService.confirmReservation(orderId));
    }

    @PutMapping("/reservations/{orderId}/release")
    @Operation(summary = "Release reservation", description = "Releases a stock reservation")
    public ResponseEntity<Void> releaseReservation(@PathVariable UUID orderId) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reservations/{orderId}")
    @Operation(summary = "Get reservations by order ID", description = "Retrieves reservations for an order")
    public ResponseEntity<?> getReservationsByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(inventoryService.getReservationsByOrderId(orderId));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items", description = "Retrieves items with low stock levels")
    public ResponseEntity<?> getLowStockItems() {
        return ResponseEntity.ok(inventoryService.getLowStockItems());
    }

    @GetMapping("/{productId}/movements")
    @Operation(summary = "Get stock movement history", description = "Retrieves stock movement history for a product")
    public ResponseEntity<?> getStockMovementHistory(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getStockMovementHistory(productId));
    }
    
    
    
    
}
