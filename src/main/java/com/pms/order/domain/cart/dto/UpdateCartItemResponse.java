package com.pms.order.domain.cart.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCartItemResponse {

    private Long cartItemId;
    private Long productId;
    private int quantity;
}
