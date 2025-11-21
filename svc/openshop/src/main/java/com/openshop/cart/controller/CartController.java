package com.openshop.cart.controller;

import com.openshop.cart.dto.AddCartItemRequestDTO;
import com.openshop.cart.dto.CartItemDTO;
import com.openshop.cart.exception.InsufficientStockException;
import com.openshop.cart.model.Cart;
import com.openshop.cart.model.CartItem;
import com.openshop.cart.service.CartService;
import com.openshop.cart.dto.CartDTO;
import com.openshop.inventory.model.Inventory;
import com.openshop.inventory.service.InventoryService;
import com.openshop.order.exception.OrderPlacementException;
import com.openshop.order.service.OrderService;
import com.openshop.order.model.Order;
import com.openshop.user.jwt.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart", description = "APIs for managing shopping cart operations")
@SecurityRequirement(name = "bearer-jwt")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final InventoryService inventoryService;

    @GetMapping
    @Operation(
        summary = "Get user's shopping cart",
        description = "Retrieve the current user's shopping cart with all items. Only accessible to CUSTOMER role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart successfully retrieved",
                     content = @Content(schema = @Schema(implementation = Cart.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role",
                     content = @Content)
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Cart> getCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching cart for userId: {}", userId);
        Cart cart = cartService.getCartByUserId(userId);
        log.debug("Cart retrieved for userId: {} with {} items", userId, cart.getItems() != null ? cart.getItems().size() : 0);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @Operation(
        summary = "Add item to cart",
        description = "Add a product with specified quantity to the shopping cart."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item successfully added to cart",
                     content = @Content(schema = @Schema(implementation = Cart.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content)
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Cart> addItem(@Valid @RequestBody AddCartItemRequestDTO dto) {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Adding item to cart for userId: {}, productId: {}, quantity: {}", 
                 userId, dto.getProductId(), dto.getQuantity());
        
        CartItem item = CartItem.builder()
                .productId(dto.getProductId())
                .quantity(dto.getQuantity())
                .build();
        
        Cart cart = cartService.addItem(userId, item);
        log.info("Item successfully added to cart for userId: {}", userId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(
        summary = "Remove item from cart",
        description = "Remove a specific item from the shopping cart by item ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item successfully removed from cart",
                     content = @Content(schema = @Schema(implementation = Cart.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Cart item not found",
                     content = @Content)
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Cart> removeItem(@Parameter(description = "Cart Item ID", required = true) @PathVariable Long itemId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Removing item from cart for userId: {}, itemId: {}", userId, itemId);
        Cart cart = cartService.removeItem(userId, itemId);
        log.info("Item successfully removed from cart for userId: {}", userId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items")
    @Operation(
        summary = "Clear shopping cart",
        description = "Remove all items from the shopping cart."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cart successfully cleared"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role",
                     content = @Content)
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Cart> clearCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Clearing cart for userId: {}", userId);
        cartService.clearCart(userId);
        Cart cart = cartService.getCartByUserId(userId);
        log.info("Cart successfully cleared for userId: {}", userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/checkout")
    @Operation(
        summary = "Checkout cart",
        description = "Process cart checkout and create an order. Validates inventory availability before placing the order."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order successfully placed",
                     content = @Content(schema = @Schema(implementation = Order.class))),
        @ApiResponse(responseCode = "400", description = "Cart is empty or insufficient stock",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role",
                     content = @Content),
        @ApiResponse(responseCode = "500", description = "Failed to place order",
                     content = @Content)
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> checkout() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Checkout initiated for userId: {}", userId);
        
        Cart cart = cartService.getCartByUserId(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Checkout failed for userId: {} - Cart is empty", userId);
            throw new IllegalArgumentException("Cart is empty. Cannot checkout.");
        }

        // Validate stock availability
        for (CartItem item : cart.getItems()) {
            try {
                Inventory inventory = inventoryService.getByProductId(item.getProductId());
                int available = inventory.getQuantity() - inventory.getReservedQuantity();
                if (available < item.getQuantity()) {
                    log.warn("Insufficient stock for product: {}, available: {}, requested: {}", 
                             item.getProductId(), available, item.getQuantity());
                    throw new InsufficientStockException(
                        String.format("Insufficient stock for product: %s. Available: %d, Requested: %d", 
                                      item.getProductId(), available, item.getQuantity()));
                }
            } catch (InsufficientStockException e) {
                throw e; // Re-throw custom exceptions
            } catch (Exception e) {
                log.error("Error checking inventory for product: {}", item.getProductId(), e);
                throw new OrderPlacementException("Failed to verify inventory availability", e);
            }
        }

        log.info("Placing order for userId: {} with {} items", userId, cart.getItems().size());
        
        // Convert Cart to CartDTO for OrderService
        CartDTO cartDTO = CartDTO.builder()
            .userId(userId)
            .items(new ArrayList<>(
                cart.getItems().stream()
                    .map(item -> CartItemDTO.builder()
                        .productId(item.getProductId().toString())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                    .toList()
            )).build();

        try {
            // Direct method call to OrderService
            Order order = orderService.placeCartOrder(cartDTO);
            log.info("Order successfully placed for userId: {} with orderId: {}", userId, order.getId());
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to place order for userId: {} - Error: {}", userId, e.getMessage(), e);
            throw new OrderPlacementException("Failed to place order: " + e.getMessage(), e);
        }
    }
}
