package com.openshop.product.graphql.type;

public record PageInfo(
    Boolean hasNextPage,
    Boolean hasPreviousPage,
    String startCursor,
    String endCursor
) {
}
