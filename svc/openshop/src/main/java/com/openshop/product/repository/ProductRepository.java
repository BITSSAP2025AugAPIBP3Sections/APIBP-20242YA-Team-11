package com.openshop.product.repository;

import com.openshop.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Product entity
 * Added pagination support
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findBySellerId(Long sellerId);
    
    /**
     * Find products by seller ID with pagination support
     * Added for pagination
     */
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
}
