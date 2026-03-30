package com.pms.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    @Builder.Default
    private String eventType = "ORDER_CREATED";
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private OrderCreatedData data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreatedData {
        private Long orderId;
        private String orderNumber;
        private Long memberId;
        private BigDecimal totalAmount;
        private int itemCount;
        private List<Map<String, Object>> items;
    }
}
