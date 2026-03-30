package com.pms.order.integration;

import com.pms.order.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[통합] 카테고리 API")
class CategoryApiTest extends IntegrationTestBase {

    @Test
    @DisplayName("카테고리 목록을 트리 구조로 조회한다")
    void should_return_categories_as_tree() throws Exception {
        // given
        Long parentId = testDataHelper.createCategory("전자제품", null, 0, 1);
        testDataHelper.createCategory("스마트폰", parentId, 1, 1);
        testDataHelper.createCategory("노트북", parentId, 1, 2);
        testDataHelper.createCategory("의류", null, 0, 2);

        // when & then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("전자제품"))
                .andExpect(jsonPath("$[0].depth").value(0))
                .andExpect(jsonPath("$[0].children.length()").value(2))
                .andExpect(jsonPath("$[0].children[0].name").value("스마트폰"))
                .andExpect(jsonPath("$[0].children[0].depth").value(1))
                .andExpect(jsonPath("$[1].name").value("의류"))
                .andExpect(jsonPath("$[1].children").isEmpty());
    }

    @Test
    @DisplayName("카테고리가 없으면 빈 배열을 반환한다")
    void should_return_empty_when_no_categories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
