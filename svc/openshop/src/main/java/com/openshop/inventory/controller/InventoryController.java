package com.openshop.inventory.controller;

import com.openshop.inventory.dto.InventoryRequest;
import com.openshop.inventory.exception.UnauthorizedException;
import com.openshop.inventory.model.Inventory;
import com.openshop.inventory.model.InventoryOperation;
import com.openshop.inventory.service.InventoryService;
import com.openshop.product.service.ProductService;
import com.openshop.user.service.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "APIs for managing product inventory and stock levels")
@SecurityRequirement(name = "bearer-jwt")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;

    /**
     * Increase inventory stock (PATCH /api/inventory/{productId}/increase)
     * Returns 200 OK
     */
    @PatchMapping("/{productId}/increase")
    @Operation(
        summary = "Increase product stock",
        description = "Increase the available stock quantity for a product. Only the product owner (seller) can increase stock."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock successfully increased",
                     content = @Content(schema = @Schema(implementation = Inventory.class))),
        @ApiResponse(responseCode = "400", description = "Invalid quantity or request parameters",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not the product owner or requires SELLER role",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Inventory not found",
                     content = @Content)
    })
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Inventory> increaseStock(
            @Parameter(description = "Product ID", required = true) @PathVariable String productId,
            @Parameter(description = "Quantity to increase", required = true) @RequestParam Integer quantity) {

        log.info("Increase stock request - productId: {}, quantity: {}", productId, quantity);

        Long authenticatedUserId = null;
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails && userDetails instanceof UserDetailsImpl authenticatedUser) {
                authenticatedUserId = authenticatedUser.getUser().getId();
            }

        if (authenticatedUserId == null) {
            log.warn("Authenticated user not found");
            throw new UnauthorizedException("User not authenticated");
        }
        
        java.util.UUID productUuid = java.util.UUID.fromString(productId);
        Long productOwnerId = productService.getProductById(productUuid).getSellerId();
        if (!authenticatedUserId.equals(productOwnerId)) {
            log.warn("User {} does not own product {}", authenticatedUserId, productId);
            throw new UnauthorizedException("You do not own this product");
        }

        InventoryRequest req = InventoryRequest.builder()
                .productId(productUuid)
                .quantity(quantity)
                .operation(InventoryOperation.INCREASE)
                .build();
                
        Inventory updated = inventoryService.addStock(req, authenticatedUserId);
        log.info("Stock increased successfully - productId: {}, newQuantity: {}", productId, updated.getQuantity());
        return ResponseEntity.ok(updated);
    }

    /**
     * Decrease inventory stock (PATCH /api/inventory/{productId}/decrease)
     * Returns 200 OK
     */
    @PatchMapping("/{productId}/decrease")
    @Operation(
        summary = "Decrease product stock",
        description = "Decrease the available stock quantity for a product. Only the product owner (seller) can decrease stock."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock successfully decreased",
                     content = @Content(schema = @Schema(implementation = Inventory.class))),
        @ApiResponse(responseCode = "400", description = "Invalid quantity, insufficient stock, or request parameters",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not the product owner or requires SELLER role",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Inventory not found",
                     content = @Content)
    })
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Inventory> decreaseStock(
            @Parameter(description = "Product ID", required = true) @PathVariable String productId,
            @Parameter(description = "Quantity to decrease", required = true) @RequestParam Integer quantity) {

        log.info("Decrease stock request - productId: {}, quantity: {}", productId, quantity);

        Long authenticatedUserId = null;
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails && userDetails instanceof UserDetailsImpl authenticatedUser) {
                authenticatedUserId = authenticatedUser.getUser().getId();
            }

        if (authenticatedUserId == null) {
            log.warn("Authenticated user not found");
            throw new UnauthorizedException("User not authenticated");
        }
        
        java.util.UUID productUuid = java.util.UUID.fromString(productId);
        Long productOwnerId = productService.getProductById(productUuid).getSellerId();
        if (!authenticatedUserId.equals(productOwnerId)) {
            log.warn("User {} does not own product {}", authenticatedUserId, productId);
            throw new UnauthorizedException("You do not own this product");
        }

        InventoryRequest req = InventoryRequest.builder()
                .productId(productUuid)
                .quantity(quantity)
                .operation(InventoryOperation.DECREASE)
                .build();
                
        Inventory updated = inventoryService.reduceStock(req);
        log.info("Stock reduced successfully - productId: {}, newQuantity: {}", productId, updated.getQuantity());
        return ResponseEntity.ok(updated);
    }



    @GetMapping("/{productId}")
    @Operation(
        summary = "Get inventory by product ID",
        description = "Retrieve inventory information for a specific product. Anyone can view inventory levels."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory successfully retrieved",
                     content = @Content(schema = @Schema(implementation = Inventory.class))),
        @ApiResponse(responseCode = "404", description = "Inventory not found",
                     content = @Content)
    })
    public ResponseEntity<Inventory> getInventory(
            @Parameter(description = "Product ID", required = true) @PathVariable String productId) {
        log.debug("Get inventory request - productId: {}", productId);
        // Anyone can view inventory levels (read-only)
        return inventoryService.getByProductId(productId)
                .map(inventory -> {
                    log.debug("Inventory found - productId: {}, quantity: {}", productId, inventory.getQuantity());
                    return ResponseEntity.ok(inventory);
                })
                .orElseGet(() -> {
                    log.debug("Inventory not found - productId: {}", productId);
                    return ResponseEntity.notFound().build();
                });
    }


}
