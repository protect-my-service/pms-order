package com.pms.order.domain.cart.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddCartItemResponse {

    private Long cartItemId;
    private Long productId;
    private String productName;
    private int quantity;
}
