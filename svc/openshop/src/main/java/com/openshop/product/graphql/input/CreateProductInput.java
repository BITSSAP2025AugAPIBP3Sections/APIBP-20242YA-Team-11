package com.openshop.product.graphql.input;

import com.openshop.product.model.Product;
import com.openshop.product.model.ProductStatus;

/**
 * GraphQL input for creating a product
 * Note: Bean Validation annotations don't work on records, so validation is done manually in the resolver
 */
public record CreateProductInput(
    String name,
    String description,
    String category,
    Double price,
    String currency,
    String sku,
    String imageUrl
) {
    public Product toProduct(Long sellerId) {
        return Product.builder()
            .name(name)
            .description(description)
            .category(category)
            .price(price)
            .currency(currency)
            .sku(sku)
            .imageUrl(imageUrl)
            .sellerId(sellerId)
            .status(ProductStatus.ACTIVE)
            .build();
    }
}
