package com.openshop.product.graphql.input;

import com.openshop.product.model.ProductStatus;

public record ProductFilter(
    String category,
    ProductStatus status,
    Double minPrice,
    Double maxPrice,
    String searchTerm
) {
}
