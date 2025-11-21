package com.openshop.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

/**
 * DTO for initiating payment requests
 * This DTO ensures no ID is sent with POST requests
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentRequestDTO {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotNull(message = "User ID is required")
    private Long userId;

    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|INITIATED)$", message = "Status must be either PENDING or INITIATED")
    private String status;
}
