package com.pms.order.domain.cart.dto;

import com.pms.order.domain.cart.entity.CartItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {

    private Long cartItemId;
    private CartProductResponse product;
    private int quantity;

    public static CartItemResponse from(CartItem cartItem) {
        return CartItemResponse.builder()
                .cartItemId(cartItem.getId())
                .product(CartProductResponse.from(cartItem.getProduct()))
                .quantity(cartItem.getQuantity())
                .build();
    }
}
