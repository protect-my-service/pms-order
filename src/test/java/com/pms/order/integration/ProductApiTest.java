package com.pms.order.integration;

import com.pms.order.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[통합] 상품 API")
class ProductApiTest extends IntegrationTestBase {

    private Long categoryId;

    @BeforeEach
    void setUp() {
        categoryId = testDataHelper.createCategory("전자제품", null, 0, 1);
    }

    @Test
    @DisplayName("카테고리별 상품 목록을 페이지네이션하여 조회한다")
    void should_return_products_by_category_with_pagination() throws Exception {
        // given
        for (int i = 1; i <= 25; i++) {
            testDataHelper.createProduct(categoryId, "상품-" + i, BigDecimal.valueOf(10000 + i), 100, "ON_SALE");
        }

        // when & then
        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", categoryId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("상품 상세 정보를 조회한다")
    void should_return_product_detail() throws Exception {
        // given
        Long productId = testDataHelper.createProduct(categoryId, "테스트상품", BigDecimal.valueOf(29900), 50, "ON_SALE");

        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("테스트상품"))
                .andExpect(jsonPath("$.price").value(29900.00))
                .andExpect(jsonPath("$.stockQuantity").value(50))
                .andExpect(jsonPath("$.status").value("ON_SALE"))
                .andExpect(jsonPath("$.category.name").value("전자제품"));
    }

    @Test
    @DisplayName("categoryId 없이 전체 상품 목록을 조회한다")
    void should_return_all_products_without_category_filter() throws Exception {
        // given
        Long categoryId2 = testDataHelper.createCategory("의류", null, 0, 2);
        testDataHelper.createProduct(categoryId, "전자제품A", BigDecimal.valueOf(10000), 10, "ON_SALE");
        testDataHelper.createProduct(categoryId2, "의류A", BigDecimal.valueOf(20000), 20, "ON_SALE");

        // when & then
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 404를 반환한다")
    void should_return_404_when_product_not_found() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}
