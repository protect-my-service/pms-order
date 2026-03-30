package com.pms.order.domain.cart.service;

import com.pms.order.domain.cart.dto.*;
import com.pms.order.domain.cart.entity.Cart;
import com.pms.order.domain.cart.entity.CartItem;
import com.pms.order.domain.cart.repository.CartItemRepository;
import com.pms.order.domain.cart.repository.CartRepository;
import com.pms.order.domain.product.entity.Product;
import com.pms.order.domain.product.repository.ProductRepository;
import com.pms.order.global.exception.BusinessException;
import com.pms.order.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart(Long memberId) {
        Cart cart = cartRepository.findByMemberIdWithItems(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(cart.getItems().stream().map(CartItemResponse::from).toList())
                .totalAmount(totalAmount)
                .build();
    }

    @Transactional
    public AddCartItemResponse addItem(Long memberId, Long productId, int quantity) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isAvailable()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            cartItem.addQuantity(quantity);
        } else {
            cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(cartItem);
        }

        return AddCartItemResponse.builder()
                .cartItemId(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .quantity(cartItem.getQuantity())
                .build();
    }

    @Transactional
    public UpdateCartItemResponse updateItemQuantity(Long memberId, Long cartItemId, int quantity) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .filter(item -> item.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItem.updateQuantity(quantity);

        return UpdateCartItemResponse.builder()
                .cartItemId(cartItem.getId())
                .productId(cartItem.getProduct().getId())
                .quantity(cartItem.getQuantity())
                .build();
    }

    @Transactional
    public void removeItem(Long memberId, Long cartItemId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .filter(item -> item.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(cartItem);
    }
}
