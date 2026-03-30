package com.pms.order.integration;

import com.pms.order.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[통합] 주문 API")
class OrderApiTest extends IntegrationTestBase {

    private Long memberId;
    private Long cartId;
    private Long categoryId;
    private Long productId;
    private Long cartItemId;

    @BeforeEach
    void setUp() {
        memberId = testDataHelper.createMember("order-test@test.com", "OrderUser");
        cartId = testDataHelper.createCart(memberId);
        categoryId = testDataHelper.createCategory("카테고리", null, 0, 1);
        productId = testDataHelper.createProduct(categoryId, "테스트상품", BigDecimal.valueOf(29900), 100, "ON_SALE");
        cartItemId = testDataHelper.createCartItem(cartId, productId, 2);
    }

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("장바구니 상품으로 주문을 생성하면 재고가 차감되고 장바구니에서 제거된다")
        void should_create_order_deduct_stock_and_remove_cart_items() throws Exception {
            // when
            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"cartItemIds": [%d]}
                                    """.formatted(cartItemId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].productName").value("테스트상품"))
                    .andExpect(jsonPath("$.items[0].quantity").value(2));

            // then: JPA 캐시를 비우고 DB에서 직접 확인
            flushAndClear();

            int remainingStock = testDataHelper.getProductStock(productId);
            assertThat(remainingStock).isEqualTo(98);

            int cartItemCount = testDataHelper.countCartItems(cartId);
            assertThat(cartItemCount).isZero();
        }

        @Test
        @DisplayName("여러 상품을 한 번에 주문할 수 있다")
        void should_create_order_with_multiple_items() throws Exception {
            // given
            Long productId2 = testDataHelper.createProduct(categoryId, "상품B", BigDecimal.valueOf(15000), 50, "ON_SALE");
            Long cartItemId2 = testDataHelper.createCartItem(cartId, productId2, 1);

            // when & then
            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"cartItemIds": [%d, %d]}
                                    """.formatted(cartItemId, cartItemId2)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.items.length()").value(2))
                    .andExpect(jsonPath("$.totalAmount").value(74800.00));
        }

        @Test
        @DisplayName("재고가 부족한 상품을 주문하면 409를 반환한다")
        void should_return_409_when_insufficient_stock() throws Exception {
            // given: 재고 1개인 상품에 수량 2로 주문
            Long lowStockProduct = testDataHelper.createProduct(categoryId, "재고부족", BigDecimal.valueOf(10000), 1, "ON_SALE");
            Long lowStockCartItem = testDataHelper.createCartItem(cartId, lowStockProduct, 2);

            // when & then
            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"cartItemIds": [%d]}
                                    """.formatted(lowStockCartItem)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        }

        @Test
        @DisplayName("품절 상품을 주문하면 409를 반환한다")
        void should_return_409_when_product_sold_out() throws Exception {
            // given
            Long soldOutProduct = testDataHelper.createProduct(categoryId, "품절", BigDecimal.valueOf(10000), 0, "SOLD_OUT");
            Long soldOutCartItem = testDataHelper.createCartItem(cartId, soldOutProduct, 1);

            // when & then
            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"cartItemIds": [%d]}
                                    """.formatted(soldOutCartItem)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));
        }

        @Test
        @DisplayName("빈 장바구니 항목으로 주문하면 400를 반환한다")
        void should_return_400_when_empty_cart_items() throws Exception {
            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"cartItemIds": []}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("주문 조회")
    class GetOrder {

        @Test
        @DisplayName("주문 상세를 조회한다")
        void should_return_order_detail() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000001", memberId, "PENDING", BigDecimal.valueOf(59800));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 2);

            // when & then
            mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value("ORD-20250401-000001"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.items.length()").value(1));
        }

        @Test
        @DisplayName("결제 완료된 주문을 조회하면 결제 정보가 포함된다")
        void should_return_order_with_payment_info() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000010", memberId, "PAID", BigDecimal.valueOf(29900));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 1);
            testDataHelper.createPayment(orderId, "PAY-detail-test", BigDecimal.valueOf(29900), "APPROVED");

            // when & then
            mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.payment.paymentKey").value("PAY-detail-test"))
                    .andExpect(jsonPath("$.payment.status").value("APPROVED"))
                    .andExpect(jsonPath("$.payment.amount").value(29900.00));
        }

        @Test
        @DisplayName("다른 회원의 주문을 조회하면 404를 반환한다")
        void should_return_404_when_other_members_order() throws Exception {
            // given
            Long otherMemberId = testDataHelper.createMember("other@test.com", "Other");
            Long orderId = testDataHelper.createOrder("ORD-20250401-000001", otherMemberId, "PENDING", BigDecimal.valueOf(10000));

            // when & then
            mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetOrders {

        @Test
        @DisplayName("회원의 주문 목록을 페이지네이션하여 조회한다")
        void should_return_order_list_with_pagination() throws Exception {
            // given
            for (int i = 1; i <= 5; i++) {
                testDataHelper.createOrder("ORD-20250401-00000" + i, memberId, "PENDING", BigDecimal.valueOf(10000 * i));
            }

            // when & then
            mockMvc.perform(get("/api/v1/orders")
                            .header("X-Member-Id", memberId)
                            .param("page", "0")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelOrder {

        @Test
        @DisplayName("PENDING 상태의 주문을 취소하면 재고가 복원된다")
        void should_cancel_pending_order_and_restore_stock() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000001", memberId, "PENDING", BigDecimal.valueOf(59800));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 2);

            int stockBefore = testDataHelper.getProductStock(productId);

            // when
            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            // then
            flushAndClear();

            int stockAfter = testDataHelper.getProductStock(productId);
            assertThat(stockAfter).isEqualTo(stockBefore + 2);

            String orderStatus = testDataHelper.getOrderStatus(orderId);
            assertThat(orderStatus).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("PAID 상태의 주문을 취소하면 결제가 취소되고 재고가 복원된다")
        void should_cancel_paid_order_with_payment_and_restore_stock() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000002", memberId, "PAID", BigDecimal.valueOf(29900));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 1);
            testDataHelper.createPayment(orderId, "PAY-cancel-order", BigDecimal.valueOf(29900), "APPROVED");

            int stockBefore = testDataHelper.getProductStock(productId);

            // when
            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            // then
            flushAndClear();

            int stockAfter = testDataHelper.getProductStock(productId);
            assertThat(stockAfter).isEqualTo(stockBefore + 1);
        }

        @Test
        @DisplayName("PREPARING 상태의 주문은 취소할 수 없다")
        void should_not_cancel_preparing_order() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000003", memberId, "PREPARING", BigDecimal.valueOf(29900));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 1);

            // when & then
            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
        }
    }
}
