package com.openshop.product.graphql.type;

import com.openshop.product.model.Product;

public record ProductEdge(
    Product node,
    String cursor
) {
}
