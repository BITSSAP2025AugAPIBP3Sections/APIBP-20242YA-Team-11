package com.openshop.payment.controller;

import com.openshop.payment.dto.InitiatePaymentRequestDTO;
import com.openshop.payment.model.Payment;
import com.openshop.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Management", description = "APIs for managing payment processing and transactions")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Initiate a new payment (POST /api/payments)
     * Returns 201 Created with Location header
     */
    @PostMapping
    @Operation(
        summary = "Initiate a payment",
        description = "Initiate a new payment transaction for an order. Supports idempotency using Idempotency-Key header."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment successfully initiated",
                     content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content)
    })
    public ResponseEntity<Payment> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequestDTO paymentRequest, 
            @Parameter(description = "Idempotency key to prevent duplicate payments") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // Generate idempotency key if not provided
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            idempotencyKey = paymentRequest.getOrderId().toString() + "-payment";
        }
        log.info("Payment initiation request - orderId: {}, userId: {}, idempotencyKey: {}",
                paymentRequest.getOrderId(), paymentRequest.getUserId() , idempotencyKey);
        Payment payment = paymentService.initiatePayment(paymentRequest, idempotencyKey);
        log.info("Payment initiated successfully - orderId: {}, transactionId: {}, status: {}", 
                payment.getOrderId(), payment.getTransactionId(), payment.getStatus());
        
        // Return 201 Created with Location header
        return ResponseEntity.created(
            org.springframework.web.servlet.support.ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{orderId}")
                .buildAndExpand(payment.getOrderId())
                .toUri()
        ).body(payment);
    }

    /**
     * Confirm payment (PUT /api/payments/{orderId}/confirm)
     * Returns 200 OK
     */
    @PutMapping("/{orderId}/confirm")
    @Operation(
        summary = "Confirm payment",
        description = "Confirm that a payment has been successfully processed for an order."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment successfully confirmed",
                     content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                     content = @Content)
    })
    public ResponseEntity<Payment> confirmPayment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {
        log.info("Confirming payment for orderId: {}", orderId);
        Payment payment = paymentService.confirmPayment(orderId);
        log.info("Payment confirmed successfully - orderId: {}, transactionId: {}", orderId, payment.getTransactionId());
        return ResponseEntity.ok(payment);
    }

    /**
     * Mark payment as failed (PUT /api/payments/{orderId}/fail)
     * Returns 200 OK
     */
    @PutMapping("/{orderId}/fail")
    @Operation(
        summary = "Mark payment as failed",
        description = "Mark a payment transaction as failed for an order."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment marked as failed",
                     content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                     content = @Content)
    })
    public ResponseEntity<Payment> failPayment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {
        log.warn("Marking payment as failed for orderId: {}", orderId);
        Payment payment = paymentService.failPayment(orderId);
        log.warn("Payment failed - orderId: {}, transactionId: {}", orderId, payment.getTransactionId());
        return ResponseEntity.ok(payment);
    }

    /**
     * Get payment status (GET /api/payments/{orderId})
     * Returns 200 OK or 404 Not Found
     */
    @GetMapping("/{orderId}")
    @Operation(
        summary = "Get payment status",
        description = "Retrieve the payment status and details for a specific order."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment status retrieved",
                     content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                     content = @Content)
    })
    public ResponseEntity<Payment> getPaymentStatus(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {
        log.debug("Payment status check - orderId: {}", orderId);
        return paymentService.getPaymentStatus(orderId)
                .map(payment -> {
                    log.debug("Payment found - orderId: {}, status: {}", orderId, payment.getStatus());
                    return ResponseEntity.ok(payment);
                })
                .orElseGet(() -> {
                    log.debug("Payment not found - orderId: {}", orderId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Cancel/refund payment (DELETE /api/payments/{orderId})
     * Compensation endpoint for Saga pattern
     * Returns 200 OK
     */
    @DeleteMapping("/{orderId}")
    @Operation(
        summary = "Cancel/refund payment",
        description = "Cancel or refund a payment transaction. This is a compensation endpoint for the Saga pattern."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment successfully cancelled/refunded",
                     content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                     content = @Content)
    })
    public ResponseEntity<Payment> cancelPayment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {
        log.info("Payment cancellation request - orderId: {}", orderId);
        Payment payment = paymentService.cancelPayment(orderId);
        log.info("Payment cancelled/refunded - orderId: {}, transactionId: {}, status: {}", 
                orderId, payment.getTransactionId(), payment.getStatus());
        return ResponseEntity.ok(payment);
    }
}
