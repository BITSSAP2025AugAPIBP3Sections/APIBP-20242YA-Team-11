package com.openshop.product.graphql.resolver;

import com.openshop.product.exception.ProductNotFoundException;
import com.openshop.product.exception.UnauthorizedException;
import com.openshop.product.graphql.input.CreateProductInput;
import com.openshop.product.graphql.input.UpdateProductInput;
import com.openshop.product.graphql.type.ProductResponse;
import com.openshop.product.model.Product;
import com.openshop.product.service.ProductService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductMutationResolver {

    private final ProductService productService;

    @MutationMapping
    public ProductResponse createProduct(
            @Argument CreateProductInput input,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Mutation: createProduct(input={})", input);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            return ProductResponse.error("Only sellers can create products");
        }
        
        if (userId == null) {
            return ProductResponse.error("User ID is required");
        }
        
        // Manual validation for GraphQL input (Bean Validation doesn't work with records)
        String validationError = validateCreateProductInput(input);
        if (validationError != null) {
            return ProductResponse.error(validationError);
        }
        
        try {
            Product product = input.toProduct(userId);
            Product created = productService.addProduct(product);
            return ProductResponse.success(created, "Product created successfully");
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ProductResponse.error("Failed to create product: " + e.getMessage());
        }
    }

    @MutationMapping
    public ProductResponse updateProduct(
            @Argument String id,
            @Argument UpdateProductInput input,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Mutation: updateProduct(id={}, input={})", id, input);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            return ProductResponse.error("Only sellers can update products");
        }
        
        if (userId == null) {
            return ProductResponse.error("User ID is required");
        }
        
        // Manual validation for GraphQL input
        String validationError = validateUpdateProductInput(input);
        if (validationError != null) {
            return ProductResponse.error(validationError);
        }
        
        try {
            // Build updated product from input
            Product updateData = Product.builder()
                .name(input.name())
                .description(input.description())
                .category(input.category())
                .price(input.price())
                .currency(input.currency())
                .sku(input.sku())
                .imageUrl(input.imageUrl())
                .status(input.status())
                .build();
            
            Product updated = productService.updateProduct(UUID.fromString(id), updateData, userId);
            return ProductResponse.success(updated, "Product updated successfully");
        } catch (ProductNotFoundException e) {
            log.error("Product not found: {}", id, e);
            return ProductResponse.error("Product not found");
        } catch (UnauthorizedException e) {
            log.error("Unauthorized update attempt: {}", id, e);
            return ProductResponse.error("You can only update your own products");
        } catch (Exception e) {
            log.error("Error updating product", e);
            return ProductResponse.error("Failed to update product: " + e.getMessage());
        }
    }

    @MutationMapping
    public ProductResponse deleteProduct(
            @Argument String id,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Mutation: deleteProduct(id={})", id);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            return ProductResponse.error("Only sellers can delete products");
        }
        
        if (userId == null) {
            return ProductResponse.error("User ID is required");
        }
        
        try {
            productService.deleteProduct(UUID.fromString(id), userId);
            return ProductResponse.success(null, "Product deleted successfully");
        } catch (ProductNotFoundException e) {
            log.error("Product not found: {}", id, e);
            return ProductResponse.error("Product not found");
        } catch (UnauthorizedException e) {
            log.error("Unauthorized delete attempt: {}", id, e);
            return ProductResponse.error("You can only delete your own products");
        } catch (Exception e) {
            log.error("Error deleting product", e);
            return ProductResponse.error("Failed to delete product: " + e.getMessage());
        }
    }

    private Long extractUserId(DataFetchingEnvironment env) {
        try {
            Object userIdObj = env.getGraphQlContext().get("userId");
            if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                return Long.parseLong((String) userIdObj);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract user ID from context", e);
            return null;
        }
    }

    private String extractUserRole(DataFetchingEnvironment env) {
        try {
            return env.getGraphQlContext().get("role");
        } catch (Exception e) {
            log.warn("Failed to extract user role from context", e);
            return null;
        }
    }
    
    /**
     * Manual validation for CreateProductInput
     * Bean Validation doesn't work with GraphQL records, so we validate manually
     */
    private String validateCreateProductInput(CreateProductInput input) {
        // Validate name
        if (input.name() == null || input.name().trim().isEmpty()) {
            return "Product name is required";
        }
        if (input.name().length() > 200) {
            return "Product name must not exceed 200 characters";
        }
        
        // Validate description
        if (input.description() != null && input.description().length() > 2000) {
            return "Description must not exceed 2000 characters";
        }
        
        // Validate category
        if (input.category() == null || input.category().trim().isEmpty()) {
            return "Category is required";
        }
        
        // Validate price
        if (input.price() == null) {
            return "Price is required";
        }
        if (input.price() <= 0) {
            return "Price must be greater than 0";
        }
        if (input.price() > 999999.99) {
            return "Price must not exceed 999,999.99";
        }
        
        // Validate currency
        if (input.currency() == null || input.currency().trim().isEmpty()) {
            return "Currency is required";
        }
        if (input.currency().length() != 3) {
            return "Currency code must be exactly 3 characters (ISO 4217)";
        }
        
        // Validate SKU
        if (input.sku() == null || input.sku().trim().isEmpty()) {
            return "SKU is required";
        }
        if (!input.sku().matches("^[A-Z0-9-]+$")) {
            return "SKU must contain only uppercase letters, numbers, and hyphens";
        }
        if (input.sku().length() < 3 || input.sku().length() > 50) {
            return "SKU must be between 3 and 50 characters";
        }
        
        // Validate image URL
        if (input.imageUrl() != null && !input.imageUrl().trim().isEmpty()) {
            if (!input.imageUrl().matches("^(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$")) {
                return "Invalid URL format for image";
            }
            if (input.imageUrl().length() > 500) {
                return "Image URL must not exceed 500 characters";
            }
        }
        
        return null; // No validation errors
    }
    
    /**
     * Manual validation for UpdateProductInput
     */
    private String validateUpdateProductInput(UpdateProductInput input) {
        // Validate name if provided
        if (input.name() != null) {
            if (input.name().trim().isEmpty()) {
                return "Product name cannot be empty";
            }
            if (input.name().length() > 200) {
                return "Product name must not exceed 200 characters";
            }
        }
        
        // Validate description if provided
        if (input.description() != null && input.description().length() > 2000) {
            return "Description must not exceed 2000 characters";
        }
        
        // Validate price if provided
        if (input.price() != null) {
            if (input.price() <= 0) {
                return "Price must be greater than 0";
            }
            if (input.price() > 999999.99) {
                return "Price must not exceed 999,999.99";
            }
        }
        
        // Validate currency if provided
        if (input.currency() != null) {
            if (input.currency().length() != 3) {
                return "Currency code must be exactly 3 characters (ISO 4217)";
            }
        }
        
        // Validate SKU if provided
        if (input.sku() != null) {
            if (!input.sku().matches("^[A-Z0-9-]+$")) {
                return "SKU must contain only uppercase letters, numbers, and hyphens";
            }
            if (input.sku().length() < 3 || input.sku().length() > 50) {
                return "SKU must be between 3 and 50 characters";
            }
        }
        
        // Validate image URL if provided
        if (input.imageUrl() != null && !input.imageUrl().trim().isEmpty()) {
            if (!input.imageUrl().matches("^(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$")) {
                return "Invalid URL format for image";
            }
            if (input.imageUrl().length() > 500) {
                return "Image URL must not exceed 500 characters";
            }
        }
        
        return null; // No validation errors
    }
}
