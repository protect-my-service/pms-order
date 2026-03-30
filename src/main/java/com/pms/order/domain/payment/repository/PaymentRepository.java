package com.pms.order.domain.payment.repository;

import com.pms.order.domain.payment.entity.Payment;
import com.pms.order.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByOrderIdAndStatusNot(Long orderId, PaymentStatus status);
}
