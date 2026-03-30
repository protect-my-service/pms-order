package com.pms.order.integration;

import com.pms.order.domain.order.dto.CreateOrderRequest;
import com.pms.order.domain.order.service.OrderService;
import com.pms.order.global.exception.BusinessException;
import com.pms.order.support.TestDataHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("[통합] 주문 동시성 테스트")
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM payment");
        jdbcTemplate.execute("DELETE FROM order_item");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM cart_item");
        jdbcTemplate.execute("DELETE FROM cart");
        jdbcTemplate.execute("DELETE FROM product");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM member");
    }

    @Test
    @DisplayName("동시에 여러 회원이 같은 상품을 주문해도 재고가 정확히 차감된다")
    void should_correctly_deduct_stock_under_concurrent_orders() throws InterruptedException {
        // given
        Long categoryId = testDataHelper.createCategory("동시성테스트", null, 0, 1);
        Long productId = testDataHelper.createProduct(categoryId, "한정상품", BigDecimal.valueOf(10000), 10, "ON_SALE");

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Long[] memberIds = new Long[threadCount];
        Long[] cartItemIds = new Long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            memberIds[i] = testDataHelper.createMember("concurrent-" + i + "@test.com", "User" + i);
            Long cartId = testDataHelper.createCart(memberIds[i]);
            cartItemIds[i] = testDataHelper.createCartItem(cartId, productId, 1);
        }

        // when: 10명이 동시에 각각 1개씩 주문 (재고 10개)
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    CreateOrderRequest request = new CreateOrderRequest();
                    ReflectionTestUtils.setField(request, "cartItemIds", List.of(cartItemIds[idx]));
                    orderService.createOrder(memberIds[idx], request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // then: 재고 10개에 10명이 각 1개씩 → 모두 성공, 재고 0
        int remainingStock = testDataHelper.getProductStock(productId);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(remainingStock).isZero();
        assertThat(successCount.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("재고보다 많은 동시 주문이 들어오면 일부만 성공하고 재고는 음수가 되지 않는다")
    void should_not_oversell_when_orders_exceed_stock() throws InterruptedException {
        // given
        Long categoryId = testDataHelper.createCategory("초과주문테스트", null, 0, 2);
        Long productId = testDataHelper.createProduct(categoryId, "한정5개", BigDecimal.valueOf(10000), 5, "ON_SALE");

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Long[] memberIds = new Long[threadCount];
        Long[] cartItemIds = new Long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            memberIds[i] = testDataHelper.createMember("oversell-" + i + "@test.com", "OUser" + i);
            Long cartId = testDataHelper.createCart(memberIds[i]);
            cartItemIds[i] = testDataHelper.createCartItem(cartId, productId, 1);
        }

        // when: 10명이 동시에 각 1개씩 주문 (재고 5개)
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    CreateOrderRequest request = new CreateOrderRequest();
                    ReflectionTestUtils.setField(request, "cartItemIds", List.of(cartItemIds[idx]));
                    orderService.createOrder(memberIds[idx], request);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // then: 정확히 5명만 성공, 재고 0 (음수 불가)
        int remainingStock = testDataHelper.getProductStock(productId);
        assertThat(remainingStock).isGreaterThanOrEqualTo(0);
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);
    }
}
