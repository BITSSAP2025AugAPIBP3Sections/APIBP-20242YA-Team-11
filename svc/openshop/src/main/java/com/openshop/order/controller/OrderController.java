package com.openshop.order.controller;

import com.openshop.inventory.service.InventoryService;
import com.openshop.order.dto.OrderResponseDTO;
import com.openshop.order.mapper.OrderMapper;
import com.openshop.order.model.Order;
import com.openshop.order.model.OrderItem;
import com.openshop.order.service.OrderService;
import com.openshop.user.jwt.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * REST controller for order management
 * Correct HTTP status codes
 * Location headers on resource creation
 * Return DTOs instead of entities
 * Clean URL patterns (POST /api/orders instead of /api/orders/placeCart)
 * Pagination support
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing customer orders")
@SecurityRequirement(name = "bearer-jwt")
public class OrderController {

    private final OrderService orderService;
    private final InventoryService inventoryService;



    /**
     * Get orders for a specific user with pagination
     * Added pagination support
     * Returns Page<OrderResponseDTO>
     */
    @GetMapping
    @Operation(
        summary = "Get user's orders with pagination",
        description = "Retrieve a paginated list of orders for the authenticated user. Users can only view their own orders."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved orders list",
                     content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role",
                     content = @Content)
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponseDTO>> getOrdersByUserId(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC") @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching orders for userId: {}, page={}, size={}", userId, page, size);
        
        // Build pageable with sorting
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // Users can only view their own orders
        Page<Order> orders = orderService.getOrdersByUserId(userId, pageable);
        log.info("Retrieved {} orders for userId: {} (page {}/{})", 
                 orders.getNumberOfElements(), userId, page + 1, orders.getTotalPages());
        
        // Convert to DTOs
        Page<OrderResponseDTO> responseDTOs = orders.map(OrderMapper::toResponseDTO);
        
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * Get a single order by ID
     * Returns OrderResponseDTO
     */
    @GetMapping("/{orderId}")
    @Operation(
        summary = "Get order by ID",
        description = "Retrieve detailed information about a specific order. Users can only view their own orders unless they have ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order successfully retrieved",
                     content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to view this order",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found",
                     content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();
        
        log.info("Fetching order by ID: {}, userId: {}", orderId, userId);
        Order order = orderService.getOrderById(orderId);
        
        // Verify user owns the order (if not admin)
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (!isAdmin && userId != null && !order.getUserId().equals(userId)) {
            log.warn("Unauthorized access to order: {} by user: {}", orderId, userId);
            return ResponseEntity.status(403).build();
        }
        
        // Return DTO instead of entity
        OrderResponseDTO responseDTO = OrderMapper.toResponseDTO(order);
        
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Update order status (combined endpoint for cancel and admin updates)
     * Returns proper status codes (200 OK on success, 403 FORBIDDEN on unauthorized)
     * Returns OrderResponseDTO

     * Supports two use cases:
     * 1. User cancellation: User can cancel their own order (status=CANCELLED)
     * 2. Admin status update: Admin can update order to any status
     */
    @PutMapping("/{orderId}/status")
    @Operation(
        summary = "Update order status",
        description = "Update the status of an order. Customers can cancel their own orders (status=CANCELLED). Admins can update to any status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order status successfully updated",
                     content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to update this order",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found",
                     content = @Content)
    })
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId,
            @Parameter(description = "New order status (e.g., CANCELLED, SHIPPED, DELIVERED)", required = true) @RequestParam String status) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();
        
        log.info("Updating order status - orderId: {}, newStatus: {}, userId: {}, role: {}", 
                 orderId, status, userId, role);
        
        Order order = orderService.getOrderById(orderId);
        
        // Authorization logic
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isUserCancellation = "CANCELLED".equalsIgnoreCase(status) && userId != null;
        
        if (isAdmin) {
            // Admin can update to any status
            log.info("Admin updating order status to: {}", status);
        } else if (isUserCancellation && order.getUserId().equals(userId)) {
            // User can only cancel their own order
            log.info("User {} cancelling their order", userId);
            
            // Release inventory for each item (BL-007 fix)
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseReservation(item.getProductId(), item.getQuantity());
            }
        } else {
            // Unauthorized: neither admin nor owner trying to cancel
            log.warn("Unauthorized status update attempt - order: {}, user: {}, role: {}, status: {}", 
                     orderId, userId, role, status);
            // Return 403 FORBIDDEN for unauthorized access
            return ResponseEntity.status(403).build();
        }
        
        // Update order status
        Order updatedOrder = orderService.updateOrderStatus(orderId, status);
        
        log.info("Order status updated successfully: {} -> {}", orderId, status);
        
        // Return DTO instead of entity
        OrderResponseDTO responseDTO = OrderMapper.toResponseDTO(updatedOrder);
        
        return ResponseEntity.ok(responseDTO);
    }
}
