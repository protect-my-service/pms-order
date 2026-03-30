package com.pms.order.domain.category.dto;

import com.pms.order.domain.category.entity.Category;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private Long parentId;
    private int depth;
    private List<CategoryResponse> children;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .depth(category.getDepth())
                .children(category.getChildren().stream()
                        .map(CategoryResponse::from)
                        .toList())
                .build();
    }
}
