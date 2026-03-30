package com.pms.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    @Builder.Default
    private String eventType = "ORDER_PAID";
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private OrderPaidData data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderPaidData {
        private Long orderId;
        private String orderNumber;
        private String paymentKey;
        private BigDecimal amount;
        private LocalDateTime paidAt;
    }
}
