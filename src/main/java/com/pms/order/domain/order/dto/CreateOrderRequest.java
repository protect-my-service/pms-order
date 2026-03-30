package com.pms.order.domain.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "장바구니 상품을 선택해주세요.")
    private List<Long> cartItemIds;
}
