package com.pms.order.domain.order.dto;

import com.pms.order.domain.payment.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentSummary {

    private String paymentKey;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;

    public static PaymentSummary from(Payment payment) {
        return PaymentSummary.builder()
                .paymentKey(payment.getPaymentKey())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
