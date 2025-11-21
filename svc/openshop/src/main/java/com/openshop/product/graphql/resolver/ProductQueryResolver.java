package com.openshop.product.graphql.resolver;

import com.openshop.product.exception.UnauthorizedException;
import com.openshop.product.graphql.input.ProductFilter;
import com.openshop.product.graphql.input.ProductSort;
import com.openshop.product.graphql.type.PageInfo;
import com.openshop.product.graphql.type.ProductConnection;
import com.openshop.product.graphql.type.ProductEdge;
import com.openshop.product.model.Product;
import com.openshop.product.service.ProductService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductQueryResolver {

    private final ProductService productService;

    @QueryMapping
    public Product product(@Argument String id, DataFetchingEnvironment env) {
        log.info("GraphQL Query: product(id={})", id);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        return productService.getProductById(UUID.fromString(id), userId, role);
    }

    @QueryMapping
    public ProductConnection products(
            @Argument ProductFilter filter,
            @Argument ProductSort sort,
            @Argument Integer first,
            @Argument String after,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Query: products(filter={}, sort={}, first={}, after={})", 
            filter, sort, first, after);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        // For now, return a simple list wrapped in connection format
        // TODO: Implement proper cursor-based pagination
        List<Product> allProducts = productService.getAllProducts(userId, role);
        
        return new ProductConnection(
            allProducts.stream()
                .map(p -> new ProductEdge(p, p.getId().toString()))
                .toList(),
            new PageInfo(false, false, null, null),
            allProducts.size()
        );
    }

    @QueryMapping
    public List<Product> myProducts(
            @Argument ProductFilter filter,
            @Argument ProductSort sort,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Query: myProducts(filter={}, sort={})", filter, sort);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        if (!"SELLER".equalsIgnoreCase(role)) {
            throw new UnauthorizedException(
                "Only sellers can access myProducts");
        }
        
        return productService.getSellerProducts(userId);
    }

    @QueryMapping
    public ProductConnection searchProducts(
            @Argument String query,
            @Argument String category,
            @Argument Integer first,
            @Argument String after,
            DataFetchingEnvironment env) {
        
        log.info("GraphQL Query: searchProducts(query={}, category={}, first={}, after={})", 
            query, category, first, after);
        
        // Extract headers from GraphQL context
        Long userId = extractUserId(env);
        String role = extractUserRole(env);
        
        // For now, use getAllProducts and filter by search term
        // TODO: Implement proper search functionality
        List<Product> allProducts = productService.getAllProducts(userId, role);
        List<Product> filtered = allProducts.stream()
            .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(query.toLowerCase())))
            .toList();
        
        return new ProductConnection(
            filtered.stream()
                .map(p -> new ProductEdge(p, p.getId().toString()))
                .toList(),
            new PageInfo(false, false, null, null),
            filtered.size()
        );
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
}
