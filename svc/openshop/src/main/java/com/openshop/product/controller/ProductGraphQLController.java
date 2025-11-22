package com.openshop.product.controller;

import com.openshop.product.graphql.input.CreateProductInput;
import com.openshop.product.graphql.input.ProductFilter;
import com.openshop.product.graphql.input.ProductSort;
import com.openshop.product.graphql.input.UpdateProductInput;
import com.openshop.product.graphql.type.ProductConnection;
import com.openshop.product.graphql.type.ProductResponse;
import com.openshop.product.model.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * GraphQL API Documentation Controller for Product Service
 * 
 * This controller provides Swagger/OpenAPI documentation for the GraphQL endpoint.
 * All GraphQL operations are accessed via POST requests to /graphql endpoint.
 * 
 * Note: This is a documentation-only controller. Actual GraphQL operations are handled
 * by Spring GraphQL framework through ProductQueryResolver and ProductMutationResolver.
 */
@RestController
@RequestMapping("/graphql")
@Tag(name = "Product GraphQL API", description = "GraphQL API for Product Management. All requests use POST method to /graphql endpoint with a JSON body containing 'query' and optional 'variables' fields.")
public class ProductGraphQLController {

    // ==================== QUERY OPERATIONS ====================

    @Operation(
        summary = "GraphQL Query: Get Product by ID",
        description = """
            Retrieves a single product by its ID.
            
            **GraphQL Query:**
            ```graphql
            query GetProduct($id: ID!) {
              product(id: $id) {
                id
                name
                description
                category
                price
                currency
                sku
                imageUrl
                sellerId
                status
                createdAt
                updatedAt
              }
            }
            ```
            
            **Variables:**
            ```json
            {
              "id": "550e8400-e29b-41d4-a716-446655440000"
            }
            ```
            """,
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Product.class),
                examples = @ExampleObject(value = """
                    {
                      "data": {
                        "product": {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "name": "Premium Laptop",
                          "description": "High-performance laptop",
                          "category": "Electronics",
                          "price": 1299.99,
                          "currency": "USD",
                          "sku": "LAPTOP-001",
                          "imageUrl": "https://example.com/laptop.jpg",
                          "sellerId": "123",
                          "status": "ACTIVE",
                          "createdAt": "2024-01-01T10:00:00Z",
                          "updatedAt": "2024-01-01T10:00:00Z"
                        }
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found"
        )
    })
    @PostMapping(value = "/product", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void getProduct(
        @RequestBody(
            description = "GraphQL query with product ID variable",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "query": "query GetProduct($id: ID!) { product(id: $id) { id name description category price currency sku imageUrl sellerId status createdAt updatedAt } }",
                      "variables": {
                        "id": "550e8400-e29b-41d4-a716-446655440000"
                      }
                    }
                    """)
            )
        ) Map<String, Object> graphqlRequest
    ) {
        // Documentation only - actual implementation handled by ProductQueryResolver
    }

    @Operation(
        summary = "GraphQL Query: Get All Products",
        description = """
            Retrieves all products with optional filtering, sorting, and pagination.
            
            **GraphQL Query:**
            ```graphql
            query GetProducts($filter: ProductFilter, $sort: ProductSort, $first: Int, $after: String) {
              products(filter: $filter, sort: $sort, first: $first, after: $after) {
                edges {
                  node {
                    id
                    name
                    category
                    price
                    status
                  }
                  cursor
                }
                pageInfo {
                  hasNextPage
                  hasPreviousPage
                  startCursor
                  endCursor
                }
                totalCount
              }
            }
            ```
            
            **Variables (all optional):**
            ```json
            {
              "filter": {
                "category": "Electronics",
                "status": "ACTIVE",
                "minPrice": 100.0,
                "maxPrice": 2000.0
              },
              "sort": {
                "field": "PRICE",
                "direction": "ASC"
              },
              "first": 10,
              "after": "cursor-string"
            }
            ```
            """,
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProductConnection.class)
            )
        )
    })
    @PostMapping(value = "/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void getProducts(
        @RequestBody(
            description = "GraphQL query with optional filter, sort, and pagination variables",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "query": "query GetProducts($filter: ProductFilter, $sort: ProductSort, $first: Int) { products(filter: $filter, sort: $sort, first: $first) { edges { node { id name category price status } cursor } pageInfo { hasNextPage hasPreviousPage } totalCount } }",
                      "variables": {
                        "filter": {
                          "category": "Electronics",
                          "status": "ACTIVE"
                        },
                        "first": 10
                      }
                    }
                    """)
            )
        ) Map<String, Object> graphqlRequest
    ) {
        // Documentation only - actual implementation handled by ProductQueryResolver
    }

    @Operation(
        summary = "GraphQL Query: Get My Products (Seller Only)",
        description = """
            Retrieves products created by the authenticated seller.
            
            **Required Role:** SELLER
            
            **GraphQL Query:**
            ```graphql
            query GetMyProducts($filter: ProductFilter, $sort: ProductSort) {
              myProducts(filter: $filter, sort: $sort) {
                id
                name
                description
                category
                price
                currency
                sku
                status
                createdAt
                updatedAt
              }
            }
            ```
            """,
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Seller's products retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Product.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Only sellers can access this endpoint"
        )
    })
    @PostMapping(value = "/myProducts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void getMyProducts(
        @RequestBody(
            description = "GraphQL query to get seller's products",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "query": "query GetMyProducts { myProducts { id name description category price currency sku status createdAt updatedAt } }"
                    }
                    """)
            )
        ) Map<String, Object> graphqlRequest
    ) {
        // Documentation only - actual implementation handled by ProductQueryResolver
    }

    @Operation(
        summary = "GraphQL Query: Search Products",
        description = """
            Searches products by query string with optional category filter.
            
            **GraphQL Query:**
            ```graphql
            query SearchProducts($query: String!, $category: String, $first: Int, $after: String) {
              searchProducts(query: $query, category: $category, first: $first, after: $after) {
                edges {
                  node {
                    id
                    name
                    description
                    category
                    price
                    imageUrl
                    status
                  }
                  cursor
                }
                pageInfo {
                  hasNextPage
                  hasPreviousPage
                }
                totalCount
              }
            }
            ```
            
            **Variables:**
            ```json
            {
              "query": "laptop",
              "category": "Electronics",
              "first": 10
            }
            ```
            """,
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search results retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProductConnection.class)
            )
        )
    })
    @PostMapping(value = "/searchProducts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void searchProducts(
        @RequestBody(
            description = "GraphQL query with search parameters",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "query": "query SearchProducts($query: String!, $category: String) { searchProducts(query: $query, category: $category) { edges { node { id name description category price status } } totalCount } }",
                      "variables": {
                        "query": "laptop",
                        "category": "Electronics"
                      }
                    }
                    """)
            )
        ) Map<String, Object> graphqlRequest
    ) {
        // Documentation only - actual implementation handled by ProductQueryResolver
    }

    // ==================== MUTATION OPERATIONS ====================

    @Operation(
        summary = "GraphQL Mutation: Create Product (Seller Only)",
        description = """
            Creates a new product. Only sellers can create products.
            
            **Required Role:** SELLER
            
            **GraphQL Mutation:**
            ```graphql
            mutation CreateProduct($input: CreateProductInput!) {
              createProduct(input: $input) {
                success
                message
                product {
                  id
                  name
                  description
                  category
                  price
                  currency
                  sku
                  imageUrl
                  status
                  createdAt
                }
                errors {
                  field
                  message
                  code
                }
              }
            }
            ```
            
            **Variables:**
            ```json
            {
              "input": {
                "name": "Premium Laptop",
                "description": "High-performance laptop for professionals",
                "category": "Electronics",
                "price": 1299.99,
                "currency": "USD",
                "sku": "LAPTOP-001",
                "imageUrl": "https://example.com/laptop.jpg"
              }
            }
            ```
            """,
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product created successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProductResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "data": {
                        "createProduct": {
                          "success": true,
                          "message": "Product created successfully",
                          "product": {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "name": "Premium Laptop",
                            "description": "High-performance laptop for professionals",
                            "category": "Electronics",
                            "price": 1299.99,
                            "currency": "USD",
                            "sku": "LAPTOP-001",
                            "imageUrl": "https://example.com/laptop.jpg",
                            "status": "ACTIVE",
                            "createdAt": "2024-01-01T10:00:00Z"
                          },
                          "errors": null
                        }
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Only sellers can create products"
        )
    })
    @PostMapping(value = "/createProduct", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void createProduct(
        @RequestBody(
            description = "GraphQL mutation with product input",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "query": "mutation CreateProduct($input: CreateProductInput!) { createProduct(input: $input) { success message product { id name description category price currency sku imageUrl status createdAt } errors { field message code } } }",
                      "variables": {
                        "input": {
                          "name": "Premium Laptop",
                          "description": "High-performance laptop for professionals",
                          "category": "Electronics",
                          "price": 1299.99,
                          "currency": "USD",
                          "sku": "LAPTOP-001",
                          "imageUrl": "https://example.com/laptop.jpg"
                        }
                      }
                    }
                    """)
            )
        ) Map<String, Object> graphqlRequest
    ) {
        // Documentation only - actual implementation handled by ProductMutationResolver
    }

    @Operation(
        summary = "GraphQL Mutation: Update Product (Seller Only)",
        description = """
            Updates an existing product. Only the product owner can update it.
            
            **Required Role:** SELLER (must own the product)
            
            **GraphQL Mutation:**
            ```graphql
            mutation UpdateProduct($id: ID!, $input: UpdateProductInput!) {
              updateProduct(id: $id, input: $input) {
                success
                message
                product {
                  id
                  name
                  description
                  category
                  price
                  currency
                  sku
                  imageUrl
                  status
                  updatedAt
                }
                errors {
                  field
                  message
                  code
                }
              }
            }
            ```
            
            **Variables:**
            ```json
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "input": {
                "name": "Updated Product Name",
                "price": 1499.99,
                "status": "ACTIVE"
              }
            }
            ```
            """,
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product updated successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProductResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Unauthorized - Only product owner can update"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found"
        )
    })
    @PostMapping(value = "/updateProduct", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateProduct(
        @RequestBody(
            description = "GraphQL mutation with product ID and update input",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "query": "mutation UpdateProduct($id: ID!, $input: UpdateProductInput!) { updateProduct(id: $id, input: $input) { success message product { id name price status updatedAt } errors { field message } } }",
                      "variables": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "input": {
                          "name": "Updated Product Name",
                          "price": 1499.99,
                          "status": "ACTIVE"
                        }
                      }
                    }
                    """)
            )
        ) Map<String, Object> graphqlRequest
    ) {
        // Documentation only - actual implementation handled by ProductMutationResolver
    }

    @Operation(
        summary = "GraphQL Mutation: Delete Product (Seller Only)",
        description = """
            Deletes a product. Only the product owner can delete it.
            
            **Required Role:** SELLER (must own the product)
            
            **GraphQL Mutation:**
            ```graphql
            mutation DeleteProduct($id: ID!) {
              deleteProduct(id: $id) {
                success
                message
                errors {
                  field
                  message
                  code
                }
              }
            }
            ```
            
            **Variables:**
            ```json
            {
              "id": "550e8400-e29b-41d4-a716-446655440000"
            }
            ```
            """,
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product deleted successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProductResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "data": {
                        "deleteProduct": {
                          "success": true,
                          "message": "Product deleted successfully",
                          "product": null,
                          "errors": null
                        }
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Unauthorized - Only product owner can delete"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found"
        )
    })
    @PostMapping(value = "/deleteProduct", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void deleteProduct(
        @RequestBody(
            description = "GraphQL mutation with product ID",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "query": "mutation DeleteProduct($id: ID!) { deleteProduct(id: $id) { success message errors { field message } } }",
                      "variables": {
                        "id": "550e8400-e29b-41d4-a716-446655440000"
                      }
                    }
                    """)
            )
        ) Map<String, Object> graphqlRequest
    ) {
        // Documentation only - actual implementation handled by ProductMutationResolver
    }

    // ==================== GRAPHQL ENDPOINT ====================

    @Operation(
        summary = "GraphQL Endpoint",
        description = """
            Main GraphQL endpoint for all queries and mutations.
            
            This is the actual endpoint used by GraphQL clients. All GraphQL operations
            (queries and mutations) are sent as POST requests to this endpoint.
            
            **Request Format:**
            ```json
            {
              "query": "your GraphQL query or mutation here",
              "variables": {
                "variable1": "value1",
                "variable2": "value2"
              },
              "operationName": "OptionalOperationName"
            }
            ```
            
            **Authentication:**
            Include JWT token in the Authorization header: `Bearer <token>`
            
            **Available Operations:**
            
            **Queries:**
            - `product(id: ID!)` - Get product by ID
            - `products(filter, sort, first, after)` - Get all products with pagination
            - `myProducts(filter, sort)` - Get seller's products (SELLER role required)
            - `searchProducts(query, category, first, after)` - Search products
            
            **Mutations:**
            - `createProduct(input: CreateProductInput!)` - Create product (SELLER role required)
            - `updateProduct(id: ID!, input: UpdateProductInput!)` - Update product (SELLER role, owner only)
            - `deleteProduct(id: ID!)` - Delete product (SELLER role, owner only)
            
            **Example Query:**
            ```json
            {
              "query": "query GetProduct($id: ID!) { product(id: $id) { id name price status } }",
              "variables": {
                "id": "550e8400-e29b-41d4-a716-446655440000"
              }
            }
            ```
            
            **Example Mutation:**
            ```json
            {
              "query": "mutation CreateProduct($input: CreateProductInput!) { createProduct(input: $input) { success message product { id name } } }",
              "variables": {
                "input": {
                  "name": "New Product",
                  "category": "Electronics",
                  "price": 99.99,
                  "currency": "USD",
                  "sku": "PROD-001"
                }
              }
            }
            ```
            """,
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "GraphQL request processed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "data": {
                        "product": {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "name": "Premium Laptop",
                          "price": 1299.99,
                          "status": "ACTIVE"
                        }
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid GraphQL query or validation error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "errors": [
                        {
                          "message": "Validation error message",
                          "locations": [{"line": 1, "column": 10}],
                          "extensions": {
                            "classification": "ValidationError"
                          }
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void graphqlEndpoint(
        @RequestBody(
            description = "GraphQL request with query/mutation and optional variables",
            content = @Content(
                examples = {
                    @ExampleObject(
                        name = "Query Example",
                        summary = "Get product by ID",
                        value = """
                            {
                              "query": "query GetProduct($id: ID!) { product(id: $id) { id name description category price currency sku status } }",
                              "variables": {
                                "id": "550e8400-e29b-41d4-a716-446655440000"
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Mutation Example",
                        summary = "Create a new product",
                        value = """
                            {
                              "query": "mutation CreateProduct($input: CreateProductInput!) { createProduct(input: $input) { success message product { id name price } errors { field message } } }",
                              "variables": {
                                "input": {
                                  "name": "New Product",
                                  "description": "Product description",
                                  "category": "Electronics",
                                  "price": 99.99,
                                  "currency": "USD",
                                  "sku": "PROD-001",
                                  "imageUrl": "https://example.com/image.jpg"
                                }
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Search Example",
                        summary = "Search products",
                        value = """
                            {
                              "query": "query SearchProducts($query: String!, $category: String) { searchProducts(query: $query, category: $category) { edges { node { id name price } } totalCount } }",
                              "variables": {
                                "query": "laptop",
                                "category": "Electronics"
                              }
                            }
                            """
                    )
                }
            )
        ) Map<String, Object> graphqlRequest
    ) {
        // Documentation only - actual implementation handled by Spring GraphQL framework
    }
}
