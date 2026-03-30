package com.pms.order.domain.payment.client;

import com.pms.order.global.exception.BusinessException;
import com.pms.order.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class ExternalPaymentClient {

    public Map<String, Object> requestPayment(String orderNumber, BigDecimal amount) {
        try {
            // Random delay simulation (300~500ms)
            long delay = ThreadLocalRandom.current().nextLong(300, 501);

            // 2% chance of timeout (3+ seconds)
            if (ThreadLocalRandom.current().nextInt(100) < 2) {
                delay = ThreadLocalRandom.current().nextLong(3000, 5001);
                log.warn("Payment timeout simulation for order: {}", orderNumber);
            }

            Thread.sleep(delay);

            // 5% chance of failure
            if (ThreadLocalRandom.current().nextInt(100) < 5) {
                log.warn("Payment failure simulation for order: {}", orderNumber);
                throw new BusinessException(ErrorCode.PAYMENT_FAILED, "외부 결제 시스템 오류가 발생했습니다.");
            }

            String paymentKey = "PAY-" + UUID.randomUUID().toString().substring(0, 8);

            return Map.of(
                    "paymentKey", paymentKey,
                    "amount", amount,
                    "status", "APPROVED"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "결제 처리 중 인터럽트가 발생했습니다.");
        }
    }

    public void cancelPayment(String paymentKey) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(200, 401));
            log.info("Payment cancelled: {}", paymentKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "결제 취소 처리 중 오류가 발생했습니다.");
        }
    }
}
