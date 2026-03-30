package com.pms.order.domain.cart.controller;

import com.pms.order.domain.cart.dto.*;
import com.pms.order.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@RequestHeader("X-Member-Id") Long memberId) {
        return cartService.getCart(memberId);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public AddCartItemResponse addItem(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(memberId, request.getProductId(), request.getQuantity());
    }

    @PatchMapping("/items/{cartItemId}")
    public UpdateCartItemResponse updateItemQuantity(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItemQuantity(memberId, cartItemId, request.getQuantity());
    }

    @DeleteMapping("/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long cartItemId) {
        cartService.removeItem(memberId, cartItemId);
    }
}
