package com.pms.order.domain.order.dto;

import com.pms.order.domain.order.entity.Order;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderCancelResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private LocalDateTime cancelledAt;

    public static OrderCancelResponse from(Order order) {
        return OrderCancelResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .cancelledAt(LocalDateTime.now())
                .build();
    }
}
