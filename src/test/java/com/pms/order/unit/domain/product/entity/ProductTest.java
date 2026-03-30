package com.pms.order.unit.domain.product.entity;

import com.pms.order.domain.product.entity.Product;
import com.pms.order.domain.product.entity.ProductStatus;
import com.pms.order.global.exception.BusinessException;
import com.pms.order.global.exception.ErrorCode;
import com.pms.order.support.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product 엔티티 도메인 로직")
class ProductTest {

    @Nested
    @DisplayName("재고 차감")
    class DeductStock {

        @Test
        @DisplayName("요청 수량만큼 재고가 차감된다")
        void should_decrease_stock_by_requested_quantity() {
            // given
            Product product = TestFixture.product(1L, "테스트 상품", BigDecimal.valueOf(10000), 100, ProductStatus.ON_SALE);

            // when
            product.deductStock(30);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("재고를 전부 차감할 수 있다")
        void should_allow_deducting_all_stock() {
            // given
            Product product = TestFixture.product(1L, "테스트 상품", BigDecimal.valueOf(10000), 5, ProductStatus.ON_SALE);

            // when
            product.deductStock(5);

            // then
            assertThat(product.getStockQuantity()).isZero();
        }

        @Test
        @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
        void should_throw_when_insufficient_stock() {
            // given
            Product product = TestFixture.product(1L, "테스트 상품", BigDecimal.valueOf(10000), 5, ProductStatus.ON_SALE);

            // when & then
            assertThatThrownBy(() -> product.deductStock(6))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
                    });
        }

        @Test
        @DisplayName("재고 부족 시 재고는 변경되지 않는다")
        void should_not_change_stock_when_insufficient() {
            // given
            Product product = TestFixture.product(1L, "테스트 상품", BigDecimal.valueOf(10000), 5, ProductStatus.ON_SALE);

            // when
            try {
                product.deductStock(6);
            } catch (BusinessException ignored) {
            }

            // then
            assertThat(product.getStockQuantity()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("재고 복원")
    class RestoreStock {

        @Test
        @DisplayName("지정된 수량만큼 재고가 증가한다")
        void should_increase_stock_by_requested_quantity() {
            // given
            Product product = TestFixture.product(1L, "테스트 상품", BigDecimal.valueOf(10000), 50, ProductStatus.ON_SALE);

            // when
            product.restoreStock(10);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("판매 가능 여부")
    class IsAvailable {

        @Test
        @DisplayName("ON_SALE 상태이면 판매 가능하다")
        void should_be_available_when_on_sale() {
            Product product = TestFixture.product(1L, "상품", BigDecimal.valueOf(10000), 10, ProductStatus.ON_SALE);
            assertThat(product.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("SOLD_OUT 상태이면 판매 불가하다")
        void should_not_be_available_when_sold_out() {
            Product product = TestFixture.product(1L, "상품", BigDecimal.valueOf(10000), 0, ProductStatus.SOLD_OUT);
            assertThat(product.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("HIDDEN 상태이면 판매 불가하다")
        void should_not_be_available_when_hidden() {
            Product product = TestFixture.product(1L, "상품", BigDecimal.valueOf(10000), 10, ProductStatus.HIDDEN);
            assertThat(product.isAvailable()).isFalse();
        }
    }
}
