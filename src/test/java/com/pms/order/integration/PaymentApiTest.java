package com.pms.order.integration;

import com.pms.order.support.IntegrationTestBase;
import com.pms.order.support.StubExternalPaymentClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[통합] 결제 API")
class PaymentApiTest extends IntegrationTestBase {

    @Autowired
    private StubExternalPaymentClient stubExternalPaymentClient;

    private Long memberId;
    private Long productId;
    private Long categoryId;

    @AfterEach
    void tearDown() {
        stubExternalPaymentClient.setShouldFail(false);
    }

    @BeforeEach
    void setUp() {
        memberId = testDataHelper.createMember("pay-test@test.com", "PayUser");
        testDataHelper.createCart(memberId);
        categoryId = testDataHelper.createCategory("카테고리", null, 0, 1);
        productId = testDataHelper.createProduct(categoryId, "테스트상품", BigDecimal.valueOf(29900), 100, "ON_SALE");
    }

    @Nested
    @DisplayName("결제 요청")
    class RequestPayment {

        @Test
        @DisplayName("PENDING 주문에 대해 결제를 요청하면 APPROVED를 반환하고 주문 상태가 PAID로 변경된다")
        void should_approve_payment_and_change_order_to_paid() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000001", memberId, "PENDING", BigDecimal.valueOf(29900));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 1);

            // when & then
            mockMvc.perform(post("/api/v1/payments")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"orderId": %d}
                                    """.formatted(orderId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentKey").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.amount").value(29900.00));

            // then: 주문 상태가 PAID로 변경되었는지 확인
            flushAndClear();
            String orderStatus = testDataHelper.getOrderStatus(orderId);
            assertThat(orderStatus).isEqualTo("PAID");
        }

        @Test
        @DisplayName("PAID 상태의 주문에 결제를 요청하면 409를 반환한다")
        void should_return_409_when_order_already_paid() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000002", memberId, "PAID", BigDecimal.valueOf(29900));

            // when & then
            mockMvc.perform(post("/api/v1/payments")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"orderId": %d}
                                    """.formatted(orderId)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
        }

        @Test
        @DisplayName("이미 APPROVED 결제가 있는 주문에 중복 결제를 요청하면 409를 반환한다")
        void should_return_409_when_duplicate_payment() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000003", memberId, "PENDING", BigDecimal.valueOf(29900));
            testDataHelper.createPayment(orderId, "PAY-existing", BigDecimal.valueOf(29900), "APPROVED");

            // when & then
            mockMvc.perform(post("/api/v1/payments")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"orderId": %d}
                                    """.formatted(orderId)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
        }

        @Test
        @DisplayName("orderId 없이 결제를 요청하면 400를 반환한다")
        void should_return_400_when_order_id_missing() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }

    @Nested
    @DisplayName("결제 실패")
    class PaymentFailure {

        @Test
        @DisplayName("외부 결제 실패 시 FAILED 결제 이력이 저장되고 주문 상태가 CANCELLED로 변경된다")
        void should_cancel_order_and_record_failed_payment_when_external_payment_fails() throws Exception {
            // given
            stubExternalPaymentClient.setShouldFail(true);
            Long orderId = testDataHelper.createOrder("ORD-20260409-000001", memberId, "PENDING", BigDecimal.valueOf(29900));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 1);

            int stockBefore = testDataHelper.getProductStock(productId);

            // when & then
            mockMvc.perform(post("/api/v1/payments")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"orderId": %d}
                                    """.formatted(orderId)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"));

            flushAndClear();

            // 주문 CANCELLED 전이 확인
            assertThat(testDataHelper.getOrderStatus(orderId)).isEqualTo("CANCELLED");

            // FAILED 결제 이력 저장 확인
            assertThat(testDataHelper.countPaymentsByStatus(orderId, "FAILED")).isEqualTo(1);

            // 재고 복원 확인
            assertThat(testDataHelper.getProductStock(productId)).isEqualTo(stockBefore + 1);
        }

        @Test
        @DisplayName("결제 실패 후 동일 주문으로 재결제 시도하면 CANCELLED 상태이므로 409를 반환한다")
        void should_return_409_when_retrying_payment_on_cancelled_order() throws Exception {
            // given - 결제 실패로 주문 CANCELLED 처리
            stubExternalPaymentClient.setShouldFail(true);
            Long orderId = testDataHelper.createOrder("ORD-20260409-000002", memberId, "PENDING", BigDecimal.valueOf(29900));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 1);

            mockMvc.perform(post("/api/v1/payments")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"orderId": %d}
                                    """.formatted(orderId)))
                    .andExpect(status().isBadGateway());

            flushAndClear();
            stubExternalPaymentClient.setShouldFail(false);

            // when - 동일 주문으로 재결제 시도
            mockMvc.perform(post("/api/v1/payments")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"orderId": %d}
                                    """.formatted(orderId)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
        }
    }

    @Nested
    @DisplayName("결제 취소")
    class CancelPayment {

        @Test
        @DisplayName("결제를 취소하면 환불 처리되고 재고가 복원된다")
        void should_cancel_payment_refund_and_restore_stock() throws Exception {
            // given
            Long orderId = testDataHelper.createOrder("ORD-20250401-000004", memberId, "PAID", BigDecimal.valueOf(29900));
            testDataHelper.createOrderItem(orderId, productId, "테스트상품", BigDecimal.valueOf(29900), 1);
            testDataHelper.createPayment(orderId, "PAY-cancel-test", BigDecimal.valueOf(29900), "APPROVED");

            int stockBefore = testDataHelper.getProductStock(productId);

            // when & then
            mockMvc.perform(post("/api/v1/payments/{paymentKey}/cancel", "PAY-cancel-test")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reason": "단순 변심"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentKey").value("PAY-cancel-test"))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            // then: 재고 복원 + 주문 상태 REFUNDED 확인
            flushAndClear();
            int stockAfter = testDataHelper.getProductStock(productId);
            assertThat(stockAfter).isEqualTo(stockBefore + 1);

            String orderStatus = testDataHelper.getOrderStatus(orderId);
            assertThat(orderStatus).isEqualTo("REFUNDED");
        }
    }
}
