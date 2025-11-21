package com.openshop.product.service;

import com.openshop.product.exception.ProductNotFoundException;
import com.openshop.product.exception.UnauthorizedException;
import com.openshop.product.model.Product;
import com.openshop.product.repository.ProductRepository;
import com.openshop.inventory.service.InventoryService;
import com.openshop.inventory.model.Inventory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductService{

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public ProductService(ProductRepository productRepository, @Lazy InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public Product addProduct(Product product) {
        log.info("Adding new product: name={}, category={}, price={}, sellerId={}", 
                 product.getName(), product.getCategory(), product.getPrice(), product.getSellerId());
        
        Product saved = productRepository.save(product);
        log.debug("Product saved to database with ID: {}", saved.getId());

        try {
            log.info("Creating inventory record for product: {}", saved.getId());
            // Direct method call to InventoryService (no HTTP)
            Inventory inventory = Inventory.builder()
                .productId(saved.getId())
                .quantity(0)
                .reservedQuantity(0)
                .build();
            inventoryService.createInventory(inventory);
            log.info("Inventory record created successfully for product: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to create inventory record for product {}: {}", saved.getId(), e.getMessage(), e);
        }

        log.info("Product added successfully: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public Product updateProduct(UUID id, Product updated, Long sellerId) {
        log.info("Updating product: id={}, sellerId={}", id, sellerId);
        
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found for update: id={}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });
        
        log.debug("Found existing product: id={}, currentSellerId={}", existing.getId(), existing.getSellerId());

        if (!existing.getSellerId().equals(sellerId)) {
            log.warn("Unauthorized update attempt: product={}, owner={}, requester={}", 
                     id, existing.getSellerId(), sellerId);
            throw new UnauthorizedException("Unauthorized: you can only modify your own products");
        }

        log.debug("Updating product fields: price={}, description={}, category={}, status={}", 
                  updated.getPrice(), updated.getDescription(), updated.getCategory(), updated.getStatus());
        
        existing.setPrice(updated.getPrice());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setImageUrl(updated.getImageUrl());
        existing.setStatus(updated.getStatus());

        Product savedProduct = productRepository.save(existing);
        
        log.info("Product updated successfully: id={}, name={}, price={}", 
                 savedProduct.getId(), savedProduct.getName(), savedProduct.getPrice());
        
        return savedProduct;
    }

    @Transactional
    public void deleteProduct(UUID id, Long sellerId) {
        log.info("Deleting product: id={}, sellerId={}", id, sellerId);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found for deletion: id={}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });
        
        if (!product.getSellerId().equals(sellerId)) {
            log.warn("Unauthorized delete attempt: product={}, owner={}, requester={}", 
                     id, product.getSellerId(), sellerId);
            throw new UnauthorizedException("Unauthorized delete attempt");
        }
        
        productRepository.delete(product);
        
        log.info("Product deleted successfully: id={}, name={}", id, product.getName());
    }

    public List<Product> getAllProducts(Long userId, String role) {
        log.info("Retrieving all products: userId={}, role={}", userId, role);
        
        if ("SELLER".equalsIgnoreCase(role) && userId != null) {
            log.debug("Seller access: returning products for sellerId={}", userId);
            List<Product> sellerProducts = productRepository.findBySellerId(userId);
            log.info("Found {} products for seller {}", sellerProducts.size(), userId);
            return sellerProducts;
        }
        
        log.debug("Public access: returning all ACTIVE products");
        List<Product> activeProducts = productRepository.findAll().stream()
                .filter(p -> "ACTIVE".equals(p.getStatus().name()))
                .collect(Collectors.toList());
        
        log.info("Found {} ACTIVE products for public access", activeProducts.size());
        return activeProducts;
    }
    
    /**
     * Get all products with pagination support
     * Added pagination support
     */
    public Page<Product> getAllProducts(Long userId, String role, Pageable pageable) {
        log.info("Retrieving paginated products: userId={}, role={}, page={}, size={}", 
                 userId, role, pageable.getPageNumber(), pageable.getPageSize());
        
        if ("SELLER".equalsIgnoreCase(role) && userId != null) {
            log.debug("Seller access: returning paginated products for sellerId={}", userId);
            Page<Product> sellerProducts = productRepository.findBySellerId(userId, pageable);
            log.info("Found {} products for seller {} (page {}/{})", 
                     sellerProducts.getNumberOfElements(), userId, 
                     sellerProducts.getNumber() + 1, sellerProducts.getTotalPages());
            return sellerProducts;
        }
        
        log.debug("Public access: returning paginated ACTIVE products");
        // Note: This is a simplified approach. For better performance, 
        // add a findByStatus method to ProductRepository with pagination
        Page<Product> allProducts = productRepository.findAll(pageable);
        List<Product> activeProducts = allProducts.getContent().stream()
                .filter(p -> "ACTIVE".equals(p.getStatus().name()))
                .collect(Collectors.toList());
        
        Page<Product> activePage = new PageImpl<>(
                activeProducts, 
                pageable, 
                allProducts.getTotalElements()
        );
        
        log.info("Found {} ACTIVE products for public access (page {}/{})", 
                 activePage.getNumberOfElements(), 
                 activePage.getNumber() + 1, 
                 activePage.getTotalPages());
        
        return activePage;
    }

    public List<Product> getSellerProducts(Long sellerId) {
        log.info("Retrieving products for seller: {}", sellerId);
        
        List<Product> products = productRepository.findBySellerId(sellerId);
        
        log.info("Found {} products for seller {}", products.size(), sellerId);
        log.debug("Seller {} products: {}", sellerId, products);
        
        return products;
    }

    public Product getProductById(UUID id) {
        log.info("Retrieving product by ID: {}", id);
        
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found: id={}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });
    }

    public Product getProductById(UUID id, Long userId, String role) {
        log.info("Retrieving product by ID: id={}, userId={}, role={}", id, userId, role);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found: id={}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });
        
        log.debug("Found product: id={}, name={}, status={}, sellerId={}", 
                  product.getId(), product.getName(), product.getStatus(), product.getSellerId());
        
        if ("SELLER".equalsIgnoreCase(role) && userId != null) {
            if (!product.getSellerId().equals(userId)) {
                log.warn("Seller {} attempted to view product {} owned by seller {}", 
                         userId, id, product.getSellerId());
                throw new UnauthorizedException("You can only view your own products");
            }
            log.debug("Seller {} accessing their own product {}", userId, id);
            return product;
        }
        
        if (!"ACTIVE".equals(product.getStatus().name())) {
            log.warn("Attempt to access non-ACTIVE product: id={}, status={}, userId={}, role={}", 
                     id, product.getStatus(), userId, role);
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        
        log.info("Product retrieved successfully: id={}, name={}", product.getId(), product.getName());
        return product;
    }
}
