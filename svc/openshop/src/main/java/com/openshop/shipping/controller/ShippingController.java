package com.openshop.shipping.controller;

import com.openshop.shipping.model.Shipment;
import com.openshop.shipping.service.ShippingService;
import com.openshop.user.jwt.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping Management", description = "APIs for managing shipment and delivery operations")
@SecurityRequirement(name = "bearer-jwt")
public class ShippingController {

    private final ShippingService shippingService;

    /**
     * Create shipment (POST /api/shipping)
     * Returns 201 Created with Location header
     */
    @PostMapping
    @Operation(
        summary = "Create a shipment",
        description = "Create a new shipment for an order with delivery address."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Shipment successfully created",
                     content = @Content(schema = @Schema(implementation = Shipment.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found",
                     content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Shipment> createShipment(
            @Parameter(description = "Order ID", required = true) @RequestParam UUID orderId,
            @Parameter(description = "Delivery address", required = true) @RequestParam String address) {
        Long userId = SecurityUtils.getCurrentUserId();
        Shipment shipment = shippingService.createShipment(orderId, userId, address);
        
        return ResponseEntity.created(
            ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{orderId}")
                .buildAndExpand(orderId)
                .toUri()
        ).body(shipment);
    }

    /**
     * Get shipment by order ID (GET /api/shipping/{orderId})
     * Returns 200 OK or 404 Not Found
     */
    @GetMapping("/{orderId}")
    @Operation(
        summary = "Get shipment by order ID",
        description = "Retrieve shipment details for a specific order."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment successfully retrieved",
                     content = @Content(schema = @Schema(implementation = Shipment.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Shipment not found",
                     content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Shipment> getShipment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {
        Shipment shipment = shippingService.getShipmentByOrderId(orderId);
        if (shipment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(shipment);
    }

    /**
     * Update shipment status (PATCH /api/shipping/{shipmentId}/status)
     * Returns 200 OK
     */
    @PatchMapping("/{shipmentId}/status")
    @Operation(
        summary = "Update shipment status",
        description = "Update the status of a shipment. Only accessible to SELLER and ADMIN roles."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment status successfully updated",
                     content = @Content(schema = @Schema(implementation = Shipment.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status value",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER or ADMIN role",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Shipment not found",
                     content = @Content)
    })
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<Shipment> updateStatus(
            @Parameter(description = "Shipment ID", required = true) @PathVariable UUID shipmentId, 
            @Parameter(description = "New shipment status (e.g., PROCESSING, SHIPPED, DELIVERED)", required = true) @RequestParam String status) {
        Shipment shipment = shippingService.updateShipmentStatus(shipmentId, status);
        return ResponseEntity.ok(shipment);
    }
}
