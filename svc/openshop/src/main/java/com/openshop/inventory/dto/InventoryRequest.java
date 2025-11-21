package com.openshop.inventory.dto;

import com.openshop.inventory.model.InventoryOperation;
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
    private Integer quantity;

    @NotNull
    private InventoryOperation operation;

    private UUID orderId; // For tracking reservations
}
