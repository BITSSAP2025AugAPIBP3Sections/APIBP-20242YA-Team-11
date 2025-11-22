 package com.openshop.product.controller;

 import com.openshop.product.dto.CreateProductRequestDTO;
 import com.openshop.product.dto.ProductResponseDTO;
 import com.openshop.product.mapper.ProductMapper;
 import com.openshop.product.model.Product;
 import com.openshop.product.service.ProductService;
 import com.openshop.user.jwt.SecurityUtils;
 import jakarta.validation.Valid;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.data.domain.Page;
 import org.springframework.data.domain.PageRequest;
 import org.springframework.data.domain.Pageable;
 import org.springframework.data.domain.Sort;
 import org.springframework.http.ResponseEntity;
 import org.springframework.security.access.prepost.PreAuthorize;
 import org.springframework.web.bind.annotation.*;
 import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
 import io.swagger.v3.oas.annotations.Operation;
 import io.swagger.v3.oas.annotations.Parameter;
 import io.swagger.v3.oas.annotations.media.Content;
 import io.swagger.v3.oas.annotations.media.Schema;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
 import io.swagger.v3.oas.annotations.responses.ApiResponses;
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
 import io.swagger.v3.oas.annotations.tags.Tag;

 import java.net.URI;
 import java.util.UUID;

 /**
  * REST controller for product management
  * Correct HTTP status codes
  * Location headers on resource creation
  * Return DTOs instead of entities
  * Clean URL patterns (removed /update prefix)
  * Pagination support
  */
 @RestController
 @RequestMapping("/api/products")
 @RequiredArgsConstructor
 @Slf4j
 @Tag(name = "Product Management", description = "APIs for managing products in the catalog")
 @SecurityRequirement(name = "bearer-jwt")
 public class ProductController {

     private final ProductService productService;

     /**
      * Create a new product
      * Returns 201 CREATED instead of 200 OK
      * Includes Location header pointing to created resource
      * Uses CreateProductRequestDTO instead of Product entity
      */
     @PostMapping
     @Operation(
         summary = "Create a new product",
         description = "Create a new product in the catalog. Only accessible to users with SELLER role."
     )
     @ApiResponses(value = {
         @ApiResponse(responseCode = "201", description = "Product successfully created",
                      content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
         @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                      content = @Content),
         @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                      content = @Content),
         @ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER role",
                      content = @Content)
     })
     @PreAuthorize("hasRole('SELLER')")
     public ResponseEntity<ProductResponseDTO> createProduct(
             @Valid @RequestBody CreateProductRequestDTO request) {
        
         Long userId = SecurityUtils.getCurrentUserId();
         String role = SecurityUtils.getCurrentUserRole();
        
         log.info("Received request to create product: name={}, category={}, userId={}, role={}",
                  request.getName(), request.getCategory(), userId, role);
        
         // Convert DTO to entity
         Product product = Product.builder()
                 .name(request.getName())
                 .description(request.getDescription())
                 .category(request.getCategory())
                 .price(request.getPrice())
                 .currency(request.getCurrency())
                 .sku(request.getSku())
                 .imageUrl(request.getImageUrl())
                 .sellerId(userId)
                 .build();
        
         log.debug("Creating product for seller {}: {}", userId, product);
         Product createdProduct = productService.addProduct(product);
        
         log.info("Product created successfully: id={}, name={}, sellerId={}",
                  createdProduct.getId(), createdProduct.getName(), createdProduct.getSellerId());
        
         // Build Location header
         URI location = ServletUriComponentsBuilder
                 .fromCurrentRequest()
                 .path("/{id}")
                 .buildAndExpand(createdProduct.getId())
                 .toUri();
        
         // Return DTO instead of entity
         ProductResponseDTO responseDTO = ProductMapper.toResponseDTO(createdProduct);
        
         // Return 201 CREATED with Location header
         return ResponseEntity.created(location).body(responseDTO);
     }

     /**
      * Update an existing product
      * Clean URL pattern (PUT /api/products/{id} instead of PUT /api/products/update/{id})
      * Returns DTO instead of entity
      */
     @PutMapping("/{id}")
     @Operation(
         summary = "Update an existing product",
         description = "Update product details. Only the product owner (seller) can update their products."
     )
     @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Product successfully updated",
                      content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
         @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                      content = @Content),
         @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                      content = @Content),
         @ApiResponse(responseCode = "403", description = "Forbidden - Not the product owner or requires SELLER role",
                      content = @Content),
         @ApiResponse(responseCode = "404", description = "Product not found",
                      content = @Content)
     })
     @PreAuthorize("hasRole('SELLER')")
     public ResponseEntity<ProductResponseDTO> updateProduct(
             @Parameter(description = "Product ID", required = true) @PathVariable UUID id,
             @Valid @RequestBody CreateProductRequestDTO request) {
        
         Long userId = SecurityUtils.getCurrentUserId();
         String role = SecurityUtils.getCurrentUserRole();
        
         log.info("Received request to update product: id={}, userId={}, role={}", id, userId, role);
        
         // Convert DTO to entity
         Product productUpdate = Product.builder()
                 .name(request.getName())
                 .description(request.getDescription())
                 .category(request.getCategory())
                 .price(request.getPrice())
                 .currency(request.getCurrency())
                 .sku(request.getSku())
                 .imageUrl(request.getImageUrl())
                 .build();
        
         log.debug("Updating product {} for seller {}", id, userId);
         Product updatedProduct = productService.updateProduct(id, productUpdate, userId);
        
         log.info("Product updated successfully: id={}, name={}, price={}",
                  updatedProduct.getId(), updatedProduct.getName(), updatedProduct.getPrice());
        
         // Return DTO instead of entity
         ProductResponseDTO responseDTO = ProductMapper.toResponseDTO(updatedProduct);
        
         return ResponseEntity.ok(responseDTO);
     }

     /**
      * Get a single product by ID
      * Returns DTO instead of entity
      */
     @GetMapping("/{id}")
     @Operation(
         summary = "Get product by ID",
         description = "Retrieve detailed information about a specific product by its ID."
     )
     @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Product successfully retrieved",
                      content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
         @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                      content = @Content),
         @ApiResponse(responseCode = "404", description = "Product not found",
                      content = @Content)
     })
     public ResponseEntity<ProductResponseDTO> getProduct(
             @Parameter(description = "Product ID", required = true) @PathVariable UUID id) {
        
         Long userId = SecurityUtils.getCurrentUserId();
         String role = SecurityUtils.getCurrentUserRole();
        
         log.info("Received request to get product: id={}, userId={}, role={}", id, userId, role);
        
         Product product = productService.getProductById(id, userId, role);
        
         log.debug("Product retrieved: id={}, name={}, status={}",
                   product.getId(), product.getName(), product.getStatus());
        
         // Return DTO instead of entity
         ProductResponseDTO responseDTO = ProductMapper.toResponseDTO(product);
        
         return ResponseEntity.ok(responseDTO);
     }

     /**
      * Get all products with pagination support
      * Added pagination with page, size, sortBy, and sortDir parameters
      * Returns Page<ProductResponseDTO> instead of List<Product>
      */
     @GetMapping
     @Operation(
         summary = "Get all products with pagination",
         description = "Retrieve a paginated list of products. Supports sorting and filtering."
     )
     @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Successfully retrieved products list",
                      content = @Content(schema = @Schema(implementation = Page.class))),
         @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                      content = @Content)
     })
     public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
             @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
             @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size,
             @Parameter(description = "Field to sort by", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
             @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC") @RequestParam(defaultValue = "DESC") String sortDir) {
        
         Long userId = SecurityUtils.getCurrentUserId();
         String role = SecurityUtils.getCurrentUserRole();
        
         log.info("Received request to get all products: page={}, size={}, sortBy={}, sortDir={}, userId={}, role={}",
                  page, size, sortBy, sortDir, userId, role);
        
         // Build pageable with sorting
         Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
         Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
         // Get paginated products from service
         Page<Product> products = productService.getAllProducts(userId, role, pageable);
        
         log.info("Retrieved {} products (page {}/{}) for user {} with role {}",
                  products.getNumberOfElements(), page + 1, products.getTotalPages(), userId, role);
        
         // Convert Page<Product> to Page<ProductResponseDTO>
         Page<ProductResponseDTO> responseDTOs = products.map(ProductMapper::toResponseDTO);
        
         return ResponseEntity.ok(responseDTOs);
     }
 }
