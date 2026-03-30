package com.pms.order.domain.payment.controller;

import com.pms.order.domain.payment.dto.PaymentCancelRequest;
import com.pms.order.domain.payment.dto.PaymentCancelResponse;
import com.pms.order.domain.payment.dto.PaymentRequest;
import com.pms.order.domain.payment.dto.PaymentResponse;
import com.pms.order.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponse requestPayment(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody PaymentRequest request) {
        return paymentService.requestPayment(memberId, request.getOrderId());
    }

    @PostMapping("/{paymentKey}/cancel")
    public PaymentCancelResponse cancelPayment(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable String paymentKey,
            @RequestBody PaymentCancelRequest request) {
        return paymentService.cancelPayment(memberId, paymentKey, request.getReason());
    }
}
