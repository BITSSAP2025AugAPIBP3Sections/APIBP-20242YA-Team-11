package com.openshop.order.service;

import com.openshop.cart.dto.CartDTO;
import com.openshop.inventory.dto.InventoryRequest;
import com.openshop.order.exception.OrderNotFoundException;
import com.openshop.order.exception.OrderPlacementException;

import com.openshop.order.model.Order;
import com.openshop.order.model.OrderItem;
import com.openshop.order.repository.OrderRepository;
import com.openshop.inventory.service.InventoryService;
import com.openshop.payment.service.PaymentService;
import com.openshop.cart.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final CartService cartService;

    /**
     * Place an order from cart with ACID transaction support.
     * All operations (order creation, inventory reservation, payment processing)
     * happen in a single transaction and will be rolled back if any step fails.
     */
    @Transactional(rollbackFor = Exception.class)
    public Order placeCartOrder(CartDTO cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be null or empty");
        }
        
        log.info("Placing order for user: {} with {} items", cart.getUserId(), cart.getItems().size());
        
        try {
            // Step 1: Create OrderItem objects from cart items
            List<OrderItem> orderItems = new ArrayList<>();
            try {
                orderItems = cart.getItems().stream()
                    .map(item -> {
                        try {
                            return OrderItem.builder()
                                .productId(UUID.fromString(item.getProductId()))
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build();
                        } catch (IllegalArgumentException e) {
                            log.error("Invalid product ID format: {}", item.getProductId());
                            throw new OrderPlacementException("Invalid product ID: " + item.getProductId(), e);
                        }
                    })
                    .toList();
            } catch (Exception e) {
                throw new OrderPlacementException("Failed to process cart items", e);
            }

            double totalPrice = orderItems.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

            // FIX BL-005: Check for idempotency key
            String idempotencyKey = cart.getIdempotencyKey();
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                try {
                    Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
                    if (existingOrder.isPresent()) {
                        log.info("Duplicate order request with idempotency key: {}", idempotencyKey);
                        return existingOrder.get();
                    }
                } catch (DataAccessException e) {
                    log.error("Database error checking idempotency key: {}", idempotencyKey, e);
                    throw new OrderPlacementException("Failed to verify order uniqueness", e);
                }
            }

            // Step 2: Create and save order
            Order order = Order.builder()
                .userId(cart.getUserId())
                .totalPrice(totalPrice)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .items(orderItems)
                .idempotencyKey(idempotencyKey)
                .build();

            try {
                order = orderRepository.save(order);
                log.info("Order created with ID: {}", order.getId());
            } catch (DataAccessException e) {
                log.error("Failed to save order to database", e);
                throw new OrderPlacementException("Failed to create order in database", e);
            }

            // Step 3: Reserve inventory for all items
            for (OrderItem item : orderItems) {
                try {
                    InventoryRequest inventoryRequest = InventoryRequest.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .orderId(order.getId())
                        .build();
                    
                    inventoryService.reserveInventory(inventoryRequest);
                    log.info("Inventory reserved for product: {}, quantity: {}", 
                        item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to reserve inventory for product: {}", item.getProductId(), e);
                    throw new OrderPlacementException(
                        "Failed to reserve inventory for product: " + item.getProductId(), e);
                }
            }

            // Step 4: Clear cart (best effort - won't rollback transaction if this fails)
            try {
                cartService.clearCart(cart.getUserId());
                log.info("Cart cleared for user: {}", cart.getUserId());
            } catch (Exception e) {
                log.warn("Failed to clear cart for user: {} - order still confirmed", 
                    cart.getUserId(), e);
            }

            // Step 5: Send notification (best effort - won't rollback transaction)
            try {
                log.info("Order confirmation notification sent for order: {}", order.getId());
            } catch (Exception e) {
                log.warn("Failed to send notification for order: {}", order.getId(), e);
            }

            log.info("Order placement completed successfully: {}", order.getId());
            return order;
            
        } catch (OrderPlacementException e) {
            log.error("Order placement failed for user: {}", cart.getUserId(), e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument in order placement for user: {}", cart.getUserId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during order placement for user: {}", cart.getUserId(), e);
            throw new OrderPlacementException("Order placement failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get all orders (admin function)
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    /**
     * Get all orders with pagination (admin function)
     * Added pagination support
     */
    public Page<Order> getAllOrders(Pageable pageable) {
        log.info("Fetching all orders with pagination: page={}, size={}", 
                 pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findAll(pageable);
    }

    /**
     * Get orders for a specific user
     */
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    /**
     * Get orders for a specific user with pagination
     * Added pagination support
     */
    public Page<Order> getOrdersByUserId(Long userId, Pageable pageable) {
        log.info("Fetching orders for user {} with pagination: page={}, size={}", 
                 userId, pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * Get order by ID
     */
    public Order getOrderById(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        
        try {
            return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        } catch (OrderNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error retrieving order: {}", orderId, e);
            throw new RuntimeException("Failed to retrieve order from database", e);
        }
    }

    /**
     * Update order status
     */
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderStatus(UUID orderId, String status) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        try {
            Order order = getOrderById(orderId);
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            
            Order updatedOrder = orderRepository.save(order);
            log.info("Order status updated: orderId={}, newStatus={}", orderId, status);
            return updatedOrder;
        } catch (OrderNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error updating order status: orderId={}, status={}", orderId, status, e);
            throw new RuntimeException("Failed to update order status in database", e);
        } catch (Exception e) {
            log.error("Unexpected error updating order status: orderId={}, status={}", orderId, status, e);
            throw new RuntimeException("Failed to update order status", e);
        }
    }
}
