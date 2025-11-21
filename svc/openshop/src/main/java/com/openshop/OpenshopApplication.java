package com.openshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * OpenShop Monolithic Application
 * 
 * This application consolidates all microservices into a single architecture:
 * - User Service
 * - Product Service (with GraphQL)
 * - Order Service (without Saga/Camel)
 * - Payment Service
 * - Cart Service
 * - Inventory Service
 * - Notification Service
 * - Shipping Service
 * 
 * Features:
 * - Single centralized PostgreSQL database
 * - Direct method invocation (no HTTP inter-service calls)
 * - Local @Transactional for ACID compliance
 * - JWT-based authentication
 * - No API Gateway required
 */
@SpringBootApplication
@EnableTransactionManagement
public class OpenshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenshopApplication.class, args);
    }

}
