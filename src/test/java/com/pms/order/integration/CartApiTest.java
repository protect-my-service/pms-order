package com.pms.order.integration;

import com.pms.order.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[통합] 장바구니 API")
class CartApiTest extends IntegrationTestBase {

    private Long memberId;
    private Long cartId;
    private Long productId;

    @BeforeEach
    void setUp() {
        memberId = testDataHelper.createMember("cart-test@test.com", "CartUser");
        cartId = testDataHelper.createCart(memberId);
        Long categoryId = testDataHelper.createCategory("카테고리", null, 0, 1);
        productId = testDataHelper.createProduct(categoryId, "테스트상품", BigDecimal.valueOf(29900), 100, "ON_SALE");
    }

    @Nested
    @DisplayName("장바구니 상품 추가")
    class AddItem {

        @Test
        @DisplayName("장바구니에 상품을 추가하면 201을 반환한다")
        void should_add_item_to_cart() throws Exception {
            mockMvc.perform(post("/api/v1/cart/items")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"productId": %d, "quantity": 2}
                                    """.formatted(productId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(productId))
                    .andExpect(jsonPath("$.quantity").value(2));
        }

        @Test
        @DisplayName("이미 장바구니에 있는 상품을 추가하면 수량이 합산된다")
        void should_merge_quantity_when_adding_existing_product() throws Exception {
            // given
            testDataHelper.createCartItem(cartId, productId, 2);

            // when & then
            mockMvc.perform(post("/api/v1/cart/items")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"productId": %d, "quantity": 3}
                                    """.formatted(productId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.quantity").value(5));
        }

        @Test
        @DisplayName("판매 불가 상품을 추가하면 409를 반환한다")
        void should_return_409_when_product_not_available() throws Exception {
            Long categoryId = testDataHelper.createCategory("기타", null, 0, 2);
            Long soldOutProduct = testDataHelper.createProduct(categoryId, "품절상품", BigDecimal.valueOf(10000), 0, "SOLD_OUT");

            mockMvc.perform(post("/api/v1/cart/items")
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"productId": %d, "quantity": 1}
                                    """.formatted(soldOutProduct)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));
        }
    }

    @Nested
    @DisplayName("장바구니 조회")
    class GetCart {

        @Test
        @DisplayName("장바구니를 조회하면 상품 목록과 총 금액을 반환한다")
        void should_return_cart_with_items_and_total() throws Exception {
            // given
            testDataHelper.createCartItem(cartId, productId, 3);

            // when & then
            mockMvc.perform(get("/api/v1/cart")
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cartId").value(cartId))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].quantity").value(3))
                    .andExpect(jsonPath("$.totalAmount").value(89700.00));
        }
    }

    @Nested
    @DisplayName("장바구니 상품 수량 변경")
    class UpdateQuantity {

        @Test
        @DisplayName("장바구니 상품의 수량을 변경한다")
        void should_update_cart_item_quantity() throws Exception {
            // given
            Long cartItemId = testDataHelper.createCartItem(cartId, productId, 2);

            // when & then
            mockMvc.perform(patch("/api/v1/cart/items/{cartItemId}", cartItemId)
                            .header("X-Member-Id", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"quantity": 5}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantity").value(5));
        }
    }

    @Nested
    @DisplayName("장바구니 상품 삭제")
    class RemoveItem {

        @Test
        @DisplayName("장바구니에서 상품을 삭제하면 204를 반환한다")
        void should_remove_item_from_cart() throws Exception {
            // given
            Long cartItemId = testDataHelper.createCartItem(cartId, productId, 1);

            // when & then
            mockMvc.perform(delete("/api/v1/cart/items/{cartItemId}", cartItemId)
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 상품을 삭제하면 404를 반환한다")
        void should_return_404_when_item_not_found() throws Exception {
            mockMvc.perform(delete("/api/v1/cart/items/{cartItemId}", 99999)
                            .header("X-Member-Id", memberId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
        }
    }
}
