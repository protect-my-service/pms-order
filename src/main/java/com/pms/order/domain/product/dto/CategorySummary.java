package com.pms.order.domain.product.dto;

import com.pms.order.domain.category.entity.Category;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategorySummary {

    private Long id;
    private String name;

    public static CategorySummary from(Category category) {
        return CategorySummary.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
