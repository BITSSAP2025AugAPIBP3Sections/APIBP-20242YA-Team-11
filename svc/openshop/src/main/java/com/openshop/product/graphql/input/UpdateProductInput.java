package com.openshop.product.graphql.input;

import com.openshop.product.model.ProductStatus;

public record UpdateProductInput(
    String name,
    String description,
    String category,
    Double price,
    String currency,
    String sku,
    String imageUrl,
    ProductStatus status
) {
}
