package com.openshop.order.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private UUID orderId;
    private Long userId;
    private Double amount;
    private String status; // Typically "PENDING" during initiation
}
