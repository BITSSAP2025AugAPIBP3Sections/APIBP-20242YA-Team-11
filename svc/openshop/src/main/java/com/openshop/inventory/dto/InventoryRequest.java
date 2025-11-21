package com.openshop.inventory.dto;

import com.openshop.inventory.model.InventoryOperation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequest {
    @NotNull
    private UUID productId;
    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 999, message = "Quantity must not exceed 999")
    private Integer quantity;

    @NotNull
    private InventoryOperation operation;

    private UUID orderId; // For tracking reservations
}
