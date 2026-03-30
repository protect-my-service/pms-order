package com.pms.order.domain.product.dto;

import com.pms.order.domain.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductDetailResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private int stockQuantity;
    private String description;
    private String imageUrl;
    private String status;
    private CategorySummary category;
    private LocalDateTime createdAt;

    public static ProductDetailResponse from(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .description(product.getDescription() != null ? product.getDescription() : "")
                .imageUrl(product.getImageUrl() != null ? product.getImageUrl() : "")
                .status(product.getStatus().name())
                .category(CategorySummary.from(product.getCategory()))
                .createdAt(product.getCreatedAt())
                .build();
    }
}
