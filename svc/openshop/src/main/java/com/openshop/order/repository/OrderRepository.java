package com.openshop.order.repository;

import com.openshop.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order entity
 * Added pagination support
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserId(Long userId);
    
    /**
     * Find orders by user ID with pagination support
     * Added for pagination
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
