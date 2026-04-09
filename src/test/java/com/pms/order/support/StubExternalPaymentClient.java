package com.pms.order.support;

import com.pms.order.domain.payment.client.ExternalPaymentClient;
import com.pms.order.global.exception.BusinessException;
import com.pms.order.global.exception.ErrorCode;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("test")
@Primary
public class StubExternalPaymentClient extends ExternalPaymentClient {

    private boolean shouldFail = false;

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public Map<String, Object> requestPayment(String orderNumber, BigDecimal amount) {
        if (shouldFail) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "외부 결제 시스템 오류 (stub)");
        }
        String paymentKey = "PAY-TEST-" + UUID.randomUUID().toString().substring(0, 8);
        return Map.of(
                "paymentKey", paymentKey,
                "amount", amount,
                "status", "APPROVED"
        );
    }

    @Override
    public void cancelPayment(String paymentKey) {
        // 즉시 성공
    }
}
