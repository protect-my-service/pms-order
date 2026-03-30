package com.pms.order.domain.order.dto;

import com.pms.order.domain.order.entity.Order;
import com.pms.order.domain.payment.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private PaymentSummary payment;
    private LocalDateTime orderedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream().map(OrderItemResponse::from).toList())
                .orderedAt(order.getOrderedAt())
                .build();
    }

    public static OrderResponse from(Order order, Payment payment) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream().map(OrderItemResponse::from).toList())
                .payment(PaymentSummary.from(payment))
                .orderedAt(order.getOrderedAt())
                .build();
    }
}
