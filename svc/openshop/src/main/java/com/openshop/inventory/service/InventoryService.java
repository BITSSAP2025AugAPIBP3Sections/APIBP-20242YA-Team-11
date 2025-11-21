package com.openshop.inventory.service;

import com.openshop.inventory.dto.InventoryRequest;
import com.openshop.inventory.model.Inventory;
import com.openshop.inventory.repository.InventoryRepository;
import com.openshop.product.service.ProductService;
import com.openshop.product.model.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductService productService;

    public Optional<Inventory> getByProductId(String productId) {
        log.debug("Retrieving inventory for productId: {}", productId);
        return inventoryRepository.findByProductId(UUID.fromString(productId));
    }

    public Inventory getByProductId(UUID productId) {
        log.debug("Retrieving inventory for productId: {}", productId);
        return inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
    }

    @Transactional
    public Inventory createInventory(Inventory inventory) {
        log.info("Creating inventory - productId: {}, quantity: {}", 
                inventory.getProductId(), inventory.getQuantity());
        
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory created successfully - productId: {}, inventoryId: {}, quantity: {}", 
                inventory.getProductId(), saved.getId(), saved.getQuantity());
        return saved;
    }

    @Transactional
    public Inventory addStock(InventoryRequest req, Long userId) {
        UUID pid = req.getProductId();
        log.info("Adding stock - productId: {}, quantity: {}, userId: {}",
                req.getProductId(), req.getQuantity(), userId);
        
        // Get product details directly from ProductService
        Product product = productService.getProductById(pid);
        if (product == null) {
            log.warn("Cannot add stock - product not found: {}", req.getProductId());
            throw new IllegalArgumentException("Cannot add stock: Product does not exist with id " + req.getProductId());
        }
        
        // If user is a SELLER, verify they own the product
        if (userId != null &&
                (product.getSellerId() == null || !product.getSellerId().equals(userId))) {
            log.warn("Unauthorized stock addition attempt - userId: {} does not own productId: {}", userId, req.getProductId());
            throw new RuntimeException("Unauthorized: You can only modify inventory for your own products");
        }

        Inventory inv = inventoryRepository.findByProductId(pid)
                .orElseGet(() -> {
                    log.info("Creating new inventory record for productId: {}", req.getProductId());
                    return Inventory.builder().productId(pid).quantity(0).reservedQuantity(0).build();
                });
        int previousQuantity = inv.getQuantity();
        inv.setQuantity(inv.getQuantity() + req.getQuantity());
        Inventory saved = inventoryRepository.save(inv);
        log.info("Stock added successfully - productId: {}, previousQuantity: {}, addedQuantity: {}, newQuantity: {}", 
                req.getProductId(), previousQuantity, req.getQuantity(), saved.getQuantity());
        return saved;
    }

    @Transactional
    public Inventory reduceStock(InventoryRequest req) {
        UUID pid = req.getProductId();
        log.info("Reducing stock - productId: {}, quantity: {}", req.getProductId(), req.getQuantity());
        
        Inventory inv = inventoryRepository.findByProductId(pid)
                .orElseThrow(() -> {
                    log.error("Cannot reduce stock - product not found in inventory: {}", req.getProductId());
                    return new RuntimeException("Product not found in inventory");
                });
        
        if (inv.getQuantity() < req.getQuantity()) {
            log.warn("Insufficient stock - productId: {}, available: {}, requested: {}", 
                    req.getProductId(), inv.getQuantity(), req.getQuantity());
            throw new RuntimeException("Insufficient stock");
        }
        
        int previousQuantity = inv.getQuantity();
        inv.setQuantity(inv.getQuantity() - req.getQuantity());
        Inventory saved = inventoryRepository.save(inv);
        log.info("Stock reduced successfully - productId: {}, previousQuantity: {}, reducedQuantity: {}, newQuantity: {}", 
                req.getProductId(), previousQuantity, req.getQuantity(), saved.getQuantity());
        return saved;
    }

    /**
     * Reserve inventory for an order (used in order placement)
     * FIX BL-001, BL-002: Uses pessimistic locking to prevent race conditions
     */
    @Transactional
    public void reserveInventory(InventoryRequest req) {
        UUID pid = req.getProductId();
        log.info("Reserving inventory - productId: {}, quantity: {}, orderId: {}", 
                pid, req.getQuantity(), req.getOrderId());
        
        // FIX BL-001, BL-002: Use pessimistic write lock to prevent race conditions
        Inventory inv = inventoryRepository.findByProductIdWithLock(pid)
                .orElseThrow(() -> {
                    log.error("Cannot reserve inventory - product not found: {}", pid);
                    return new RuntimeException("Product not found in inventory: " + pid);
                });
        
        int availableQuantity = inv.getQuantity() - inv.getReservedQuantity();
        if (availableQuantity < req.getQuantity()) {
            log.warn("Insufficient stock for reservation - productId: {}, available: {}, requested: {}", 
                    pid, availableQuantity, req.getQuantity());
            throw new RuntimeException("Insufficient stock available for product: " + pid);
        }
        
        // Lock is held until transaction commits, preventing concurrent modifications
        inv.setReservedQuantity(inv.getReservedQuantity() + req.getQuantity());
        inventoryRepository.save(inv);
        log.info("Inventory reserved successfully - productId: {}, reservedQuantity: {}", 
                pid, req.getQuantity());
    }

    /**
     * Confirm reservation and reduce actual stock (called after payment success)
     * FIX BL-002: Uses pessimistic locking for thread safety
     */
    @Transactional
    public void confirmReservation(UUID productId, int quantity) {
        log.info("Confirming reservation - productId: {}, quantity: {}", productId, quantity);
        
        // Use pessimistic lock to prevent concurrent modifications
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new RuntimeException("Product not found in inventory: " + productId));
        
        inv.setQuantity(inv.getQuantity() - quantity);
        inv.setReservedQuantity(inv.getReservedQuantity() - quantity);
        inventoryRepository.save(inv);
        log.info("Reservation confirmed - productId: {}, newQuantity: {}, newReserved: {}", 
                productId, inv.getQuantity(), inv.getReservedQuantity());
    }

    /**
     * Release reservation (called on order failure/cancellation)
     * FIX BL-002, BL-007: Uses pessimistic locking and properly releases inventory
     */
    @Transactional
    public void releaseReservation(UUID productId, int quantity) {
        log.info("Releasing reservation - productId: {}, quantity: {}", productId, quantity);
        
        // Use pessimistic lock to prevent concurrent modifications
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new RuntimeException("Product not found in inventory: " + productId));
        
        inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - quantity));
        inventoryRepository.save(inv);
        log.info("Reservation released - productId: {}, newReserved: {}", 
                productId, inv.getReservedQuantity());
    }
}
