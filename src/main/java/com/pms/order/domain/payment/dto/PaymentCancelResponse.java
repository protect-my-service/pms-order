package com.pms.order.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentCancelResponse {

    private String paymentKey;
    private String status;
    private LocalDateTime cancelledAt;
}
