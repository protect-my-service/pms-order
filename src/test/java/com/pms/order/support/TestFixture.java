package com.pms.order.support;

import com.pms.order.domain.cart.entity.Cart;
import com.pms.order.domain.cart.entity.CartItem;
import com.pms.order.domain.member.entity.Member;
import com.pms.order.domain.order.entity.Order;
import com.pms.order.domain.order.entity.OrderItem;
import com.pms.order.domain.order.entity.OrderStatus;
import com.pms.order.domain.payment.entity.Payment;
import com.pms.order.domain.product.entity.Product;
import com.pms.order.domain.product.entity.ProductStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class TestFixture {

    public static Member member(Long id, String email, String name) {
        Member member = instantiate(Member.class);
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "email", email);
        ReflectionTestUtils.setField(member, "name", name);
        return member;
    }

    public static Product product(Long id, String name, BigDecimal price, int stockQuantity, ProductStatus status) {
        Product product = instantiate(Product.class);
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "name", name);
        ReflectionTestUtils.setField(product, "price", price);
        ReflectionTestUtils.setField(product, "stockQuantity", stockQuantity);
        ReflectionTestUtils.setField(product, "status", status);
        return product;
    }

    public static Cart cart(Long id, Member member) {
        Cart cart = instantiate(Cart.class);
        ReflectionTestUtils.setField(cart, "id", id);
        ReflectionTestUtils.setField(cart, "member", member);
        ReflectionTestUtils.setField(cart, "items", new ArrayList<>());
        return cart;
    }

    public static CartItem cartItem(Long id, Cart cart, Product product, int quantity) {
        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    public static Order order(Long id, String orderNumber, Member member, OrderStatus status, BigDecimal totalAmount) {
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .member(member)
                .totalAmount(totalAmount)
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        if (status != OrderStatus.PENDING) {
            ReflectionTestUtils.setField(order, "status", status);
        }
        ReflectionTestUtils.setField(order, "items", new ArrayList<>());
        return order;
    }

    public static OrderItem orderItem(Long id, Order order, Product product, int quantity) {
        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .productPrice(product.getPrice())
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    public static Payment payment(Long id, Order order, String paymentKey, BigDecimal amount) {
        Payment payment = Payment.builder()
                .order(order)
                .paymentKey(paymentKey)
                .amount(amount)
                .build();
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiate(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getSimpleName(), e);
        }
    }
}
