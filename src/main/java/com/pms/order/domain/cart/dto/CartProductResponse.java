package com.pms.order.domain.cart.dto;

import com.pms.order.domain.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private int stockQuantity;
    private String status;

    public static CartProductResponse from(Product product) {
        return CartProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl() != null ? product.getImageUrl() : "")
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus().name())
                .build();
    }
}
