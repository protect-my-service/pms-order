package com.pms.order.domain.product.service;

import com.pms.order.domain.product.dto.ProductDetailResponse;
import com.pms.order.domain.product.dto.ProductListResponse;
import com.pms.order.domain.product.repository.ProductRepository;
import com.pms.order.global.exception.BusinessException;
import com.pms.order.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductListResponse> getProducts(Long categoryId, Pageable pageable) {
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId, pageable).map(ProductListResponse::from);
        }
        return productRepository.findAll(pageable).map(ProductListResponse::from);
    }

    public ProductDetailResponse getProduct(Long productId) {
        return ProductDetailResponse.from(
                productRepository.findById(productId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));
    }
}
