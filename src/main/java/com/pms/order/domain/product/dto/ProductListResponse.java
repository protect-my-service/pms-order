package com.pms.order.domain.product.dto;

import com.pms.order.domain.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductListResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private int stockQuantity;
    private String imageUrl;
    private String status;
    private CategorySummary category;

    public static ProductListResponse from(Product product) {
        return ProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl() != null ? product.getImageUrl() : "")
                .status(product.getStatus().name())
                .category(CategorySummary.from(product.getCategory()))
                .build();
    }
}
