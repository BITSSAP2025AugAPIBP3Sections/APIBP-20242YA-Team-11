package com.openshop.payment.service;

import com.openshop.order.model.Order;
import com.openshop.order.repository.OrderRepository;
import com.openshop.payment.model.Payment;
import com.openshop.payment.repository.PaymentRepository;
import com.openshop.payment.dto.PaymentRequest;
import com.openshop.payment.dto.InitiatePaymentRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Process payment - simplified for monolithic architecture
     * No Camel/Saga needed - transactions handled by @Transactional
     */
    @Transactional
    public Payment processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment for order: {}, amount: {}", 
                paymentRequest.getOrderId(), paymentRequest.getAmount());

        // Check for duplicate payment
        Optional<Payment> existing = paymentRepository.findByOrderId(paymentRequest.getOrderId());
        if (existing.isPresent()) {
            log.info("Payment already exists for order: {}", paymentRequest.getOrderId());
            return existing.get();
        }

        Payment payment = Payment.builder()
                .orderId(paymentRequest.getOrderId())
                .userId(paymentRequest.getUserId())
                .amount(paymentRequest.getAmount())
                .transactionId(UUID.randomUUID().toString())
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment processed successfully - orderId: {}, transactionId: {}", 
                paymentRequest.getOrderId(), saved.getTransactionId());
        
        return saved;
    }

    /**
     * Initiate payment with idempotency support
     * Uses DTO to ensure no ID is sent with POST request
     */
    @Transactional
    public Payment initiatePayment(InitiatePaymentRequestDTO paymentRequest, String idempotencyKey) {
        // Check if payment with this idempotency key already exists
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate payment request detected for idempotency key: {}, returning existing payment", 
                    idempotencyKey);
            return existing.get();
        }

        Payment payment = Payment.builder()
                .orderId(paymentRequest.getOrderId())
                .userId(paymentRequest.getUserId())
                .amount(orderRepository.findById(paymentRequest.getOrderId()).get().getTotalPrice())
                .transactionId(UUID.randomUUID().toString())
                .status("INITIATED")
                .idempotencyKey(idempotencyKey)
                .timestamp(LocalDateTime.now())
                .build();

        // Update order status directly via repository
        Order order = orderRepository.findById(paymentRequest.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + paymentRequest.getOrderId()));
        order.setStatus("PAYMENT_INITIATED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Initiating new payment for order: {} with idempotency key: {}", 
                paymentRequest.getOrderId(), idempotencyKey);
        return paymentRepository.save(payment);
    }

    /**
     * Confirm payment (success)
     */
    @Transactional
    public Payment confirmPayment(UUID orderId) {
        log.info("Confirming payment for orderId: {}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Payment not found for orderId: {}", orderId);
                    return new RuntimeException("No payment found for order: " + orderId);
                });

        payment.setStatus("SUCCESS");
        payment.setTimestamp(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        // Update order status directly via repository
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Payment confirmed - orderId: {}, transactionId: {}", orderId, saved.getTransactionId());
        return saved;
    }

    /**
     * Mark payment as failed
     */
    @Transactional
    public Payment failPayment(UUID orderId) {
        log.warn("Failing payment for orderId: {}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("No payment found for order: " + orderId));

        payment.setStatus("FAILED");
        payment.setTimestamp(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        // Update order status directly via repository
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus("FAILED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        
        log.warn("Payment marked as failed for order: {}", orderId);
        return savedPayment;
    }

    /**
     * Cancel/Refund payment
     */
    @Transactional
    public Payment cancelPayment(UUID orderId) {
        log.info("Cancelling payment for orderId: {}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Payment not found for cancellation - orderId: {}", orderId);
                    return new RuntimeException("No payment found for order: " + orderId);
                });

        payment.setStatus("REFUNDED");
        payment.setTimestamp(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        log.info("Payment refunded successfully - orderId: {}, transactionId: {}", 
                orderId, saved.getTransactionId());
        return saved;
    }

    /**
     * Get payment status by order ID
     */
    public Optional<Payment> getPaymentStatus(UUID orderId) {
        log.debug("Fetching payment status for orderId: {}", orderId);
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Get payment by ID
     */
    public Payment getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }
}
