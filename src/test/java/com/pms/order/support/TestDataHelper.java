package com.pms.order.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TestDataHelper {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Long createMember(String email, String name) {
        jdbcTemplate.update(
                "INSERT INTO member (email, name, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                email, name);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE email = ?", Long.class, email);
    }

    public Long createCategory(String name, Long parentId, int depth, int sortOrder) {
        jdbcTemplate.update(
                "INSERT INTO category (name, parent_id, depth, sort_order, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                name, parentId, depth, sortOrder);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class, name);
    }

    public Long createProduct(Long categoryId, String name, BigDecimal price, int stockQuantity, String status) {
        jdbcTemplate.update(
                "INSERT INTO product (category_id, name, price, stock_quantity, description, image_url, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                categoryId, name, price, stockQuantity, "desc", "https://cdn.example.com/img.jpg", status);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM product WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class, name);
    }

    public Long createCart(Long memberId) {
        jdbcTemplate.update(
                "INSERT INTO cart (member_id, created_at, updated_at) VALUES (?, NOW(), NOW())",
                memberId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cart WHERE member_id = ?", Long.class, memberId);
    }

    public Long createCartItem(Long cartId, Long productId, int quantity) {
        jdbcTemplate.update(
                "INSERT INTO cart_item (cart_id, product_id, quantity, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                cartId, productId, quantity);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cart_item WHERE cart_id = ? AND product_id = ?", Long.class, cartId, productId);
    }

    public Long createOrder(String orderNumber, Long memberId, String status, BigDecimal totalAmount) {
        jdbcTemplate.update(
                "INSERT INTO orders (order_number, member_id, status, total_amount, ordered_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, NOW(), NOW(), NOW())",
                orderNumber, memberId, status, totalAmount);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM orders WHERE order_number = ?", Long.class, orderNumber);
    }

    public Long createOrderItem(Long orderId, Long productId, String productName, BigDecimal productPrice, int quantity) {
        jdbcTemplate.update(
                "INSERT INTO order_item (order_id, product_id, product_name, product_price, quantity, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())",
                orderId, productId, productName, productPrice, quantity);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM order_item WHERE order_id = ? AND product_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, orderId, productId);
    }

    public void createPayment(Long orderId, String paymentKey, BigDecimal amount, String status) {
        jdbcTemplate.update(
                "INSERT INTO payment (order_id, payment_key, amount, status, paid_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, NOW(), NOW(), NOW())",
                orderId, paymentKey, amount, status);
    }

    public int getProductStock(Long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT stock_quantity FROM product WHERE id = ?", Integer.class, productId);
    }

    public int countCartItems(Long cartId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cart_item WHERE cart_id = ?", Integer.class, cartId);
    }

    public String getOrderStatus(Long orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    public int countPaymentsByStatus(Long orderId, String status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment WHERE order_id = ? AND status = ?",
                Integer.class, orderId, status);
    }
}
