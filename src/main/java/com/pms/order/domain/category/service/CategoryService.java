package com.pms.order.domain.category.service;

import com.pms.order.domain.category.dto.CategoryResponse;
import com.pms.order.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findByParentIsNullOrderBySortOrderAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
