package com.openshop.user.config;

import com.openshop.inventory.model.Inventory;
import com.openshop.inventory.repository.InventoryRepository;
import com.openshop.order.model.Order;
import com.openshop.order.model.OrderItem;
import com.openshop.order.repository.OrderRepository;
import com.openshop.product.model.Product;
import com.openshop.product.model.ProductStatus;
import com.openshop.product.repository.ProductRepository;
import com.openshop.user.model.Role;
import com.openshop.user.model.User;
import com.openshop.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DataInitializer {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    public DataInitializer(UserService userService, 
                          PasswordEncoder passwordEncoder,
                          ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          OrderRepository orderRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Check if data already exists
            if (userService.findByUsername("seller1").isPresent()) {
                System.out.println("Data already initialized. Skipping initialization.");
                return;
            }

            System.out.println("Initializing database with sample data...");

            // Create Sellers
            User seller1 = createUserIfNotExists(
                    "seller1",
                    "John Electronics",
                    "john@electronics.com",
                    "password123",
                    Role.SELLER
            );

            User seller2 = createUserIfNotExists(
                    "seller2",
                    "Sarah Fashion",
                    "sarah@fashion.com",
                    "password123",
                    Role.SELLER
            );

            // Create Customers
            User customer1 = createUserIfNotExists(
                    "customer1",
                    "Michael Smith",
                    "michael@customer.com",
                    "password123",
                    Role.CUSTOMER
            );

            User customer2 = createUserIfNotExists(
                    "customer2",
                    "Emma Johnson",
                    "emma@customer.com",
                    "password123",
                    Role.CUSTOMER
            );

            // Create Admin (optional)
            createUserIfNotExists(
                    "admin",
                    "Admin User",
                    "admin@openshop.com",
                    "admin123",
                    Role.ADMIN
            );

            // Create Products for Seller 1 (Electronics)
            Product laptop = createProductIfNotExists(
                    "Gaming Laptop",
                    "High-performance gaming laptop with RTX 4060",
                    "electronics",
                    1299.99,
                    "USD",
                    "LAPTOP-001",
                    seller1.getId(),
                    "https://example.com/laptop.jpg"
            );

            Product smartphone = createProductIfNotExists(
                    "Smartphone Pro",
                    "Latest flagship smartphone with 5G",
                    "electronics",
                    899.99,
                    "USD",
                    "PHONE-001",
                    seller1.getId(),
                    "https://example.com/phone.jpg"
            );

            Product headphones = createProductIfNotExists(
                    "Wireless Headphones",
                    "Noise-cancelling wireless headphones",
                    "electronics",
                    199.99,
                    "USD",
                    "HEAD-001",
                    seller1.getId(),
                    "https://example.com/headphones.jpg"
            );

            // Create Products for Seller 2 (Fashion)
            Product tshirt = createProductIfNotExists(
                    "Designer T-Shirt",
                    "Premium cotton designer t-shirt",
                    "clothing",
                    49.99,
                    "USD",
                    "TSHIRT-001",
                    seller2.getId(),
                    "https://example.com/tshirt.jpg"
            );

            Product jeans = createProductIfNotExists(
                    "Slim Fit Jeans",
                    "Comfortable slim fit denim jeans",
                    "clothing",
                    79.99,
                    "USD",
                    "JEANS-001",
                    seller2.getId(),
                    "https://example.com/jeans.jpg"
            );

            Product sneakers = createProductIfNotExists(
                    "Running Sneakers",
                    "Lightweight running sneakers",
                    "clothing",
                    129.99,
                    "USD",
                    "SNEAK-001",
                    seller2.getId(),
                    "https://example.com/sneakers.jpg"
            );

            // Create Inventory for Products
            createInventoryIfNotExists(laptop.getId(), 15);
            createInventoryIfNotExists(smartphone.getId(), 25);
            createInventoryIfNotExists(headphones.getId(), 50);
            createInventoryIfNotExists(tshirt.getId(), 100);
            createInventoryIfNotExists(jeans.getId(), 75);
            createInventoryIfNotExists(sneakers.getId(), 40);

            // Create Orders for Customer 1
            createOrderIfNotExists(
                    customer1.getId(),
                    List.of(
                            createOrderItem(laptop.getId(), 1, laptop.getPrice()),
                            createOrderItem(headphones.getId(), 2, headphones.getPrice())
                    ),
                    "PLACED"
            );

            createOrderIfNotExists(
                    customer1.getId(),
                    List.of(
                            createOrderItem(smartphone.getId(), 1, smartphone.getPrice())
                    ),
                    "CONFIRMED"
            );

            // Create Orders for Customer 2
            createOrderIfNotExists(
                    customer2.getId(),
                    List.of(
                            createOrderItem(tshirt.getId(), 3, tshirt.getPrice()),
                            createOrderItem(jeans.getId(), 2, jeans.getPrice())
                    ),
                    "CONFIRMED"
            );

            createOrderIfNotExists(
                    customer2.getId(),
                    List.of(
                            createOrderItem(sneakers.getId(), 1, sneakers.getPrice()),
                            createOrderItem(tshirt.getId(), 2, tshirt.getPrice())
                    ),
                    "PLACED"
            );

            System.out.println("Database initialization completed successfully!");
            System.out.println("Created 2 sellers, 2 customers, 6 products, inventory entries, and 4 orders.");
        };
    }

    private User createUserIfNotExists(String username, String name, String email, 
                                      String password, Role role) {
        Optional<User> existingUser = userService.findByUsername(username);
        if (existingUser.isPresent()) {
            System.out.println(role.name() + " user already exists: " + username);
            return existingUser.get();
        }

        User user = User.builder()
                .username(username)
                .name(name)
                .email(email)
                .password(password)
                .role(role)
                .isActive(true)
                .build();

        User createdUser = userService.createUser(user);
        System.out.println(role.name() + " user created: " + username);
        return createdUser;
    }

    private Product createProductIfNotExists(String name, String description, String category,
                                            Double price, String currency, String sku,
                                            Long sellerId, String imageUrl) {
        // Check if product with same SKU exists
        Optional<Product> existingProduct = productRepository.findAll().stream()
                .filter(p -> p.getSku() != null && p.getSku().equals(sku))
                .findFirst();

        if (existingProduct.isPresent()) {
            System.out.println("Product already exists: " + name);
            return existingProduct.get();
        }

        Product product = Product.builder()
                .name(name)
                .description(description)
                .category(category)
                .price(price)
                .currency(currency)
                .sku(sku)
                .sellerId(sellerId)
                .imageUrl(imageUrl)
                .status(ProductStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        System.out.println("Product created: " + name);
        return savedProduct;
    }

    private Inventory createInventoryIfNotExists(UUID productId, int quantity) {
        // Check if inventory for product exists
        Optional<Inventory> existingInventory = inventoryRepository.findAll().stream()
                .filter(inv -> inv.getProductId().equals(productId))
                .findFirst();

        if (existingInventory.isPresent()) {
            System.out.println("Inventory already exists for product: " + productId);
            return existingInventory.get();
        }

        Inventory inventory = Inventory.builder()
                .productId(productId)
                .quantity(quantity)
                .reservedQuantity(0)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);
        System.out.println("Inventory created for product: " + productId + " with quantity: " + quantity);
        return savedInventory;
    }

    private OrderItem createOrderItem(UUID productId, int quantity, double price) {
        return OrderItem.builder()
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .build();
    }

    private Order createOrderIfNotExists(Long userId, List<OrderItem> items, String status) {
        // Calculate total price
        double totalPrice = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        // Generate a unique checkout batch ID
        String checkoutBatchId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8);

        Order order = Order.builder()
                .userId(userId)
                .items(items)
                .totalPrice(totalPrice)
                .status(status)
                .checkoutBatchId(checkoutBatchId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);
        System.out.println("Order created for user: " + userId + " with total: $" + totalPrice);
        return savedOrder;
    }
}
