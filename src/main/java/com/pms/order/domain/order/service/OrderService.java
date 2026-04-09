package com.pms.order.domain.order.service;

import com.pms.order.domain.cart.entity.Cart;
import com.pms.order.domain.cart.entity.CartItem;
import com.pms.order.domain.cart.repository.CartItemRepository;
import com.pms.order.domain.cart.repository.CartRepository;
import com.pms.order.domain.member.entity.Member;
import com.pms.order.domain.member.repository.MemberRepository;
import com.pms.order.domain.order.dto.*;
import com.pms.order.domain.order.entity.Order;
import com.pms.order.domain.order.entity.OrderItem;
import com.pms.order.domain.order.entity.OrderStatus;
import com.pms.order.domain.order.entity.OrderNumberSequence;
import com.pms.order.domain.order.repository.OrderNumberSequenceRepository;
import com.pms.order.domain.order.repository.OrderRepository;
import com.pms.order.domain.payment.entity.Payment;
import com.pms.order.domain.payment.repository.PaymentRepository;
import com.pms.order.domain.product.entity.Product;
import com.pms.order.domain.product.repository.ProductRepository;
import com.pms.order.event.OrderCancelledEvent;
import com.pms.order.event.OrderCreatedEvent;
import com.pms.order.global.exception.BusinessException;
import com.pms.order.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderNumberSequenceRepository orderNumberSequenceRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final EntityManager entityManager;

    @Transactional
    public OrderResponse createOrder(Long memberId, CreateOrderRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findAllByIdInAndCartId(request.getCartItemIds(), cart.getId());

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        if (cartItems.size() != request.getCartItemIds().size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND, "일부 장바구니 상품을 찾을 수 없습니다.");
        }

        String orderNumber = generateOrderNumber();

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .member(member)
                .totalAmount(BigDecimal.ZERO)
                .build();
        orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findByIdWithLock(cartItem.getProduct().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            entityManager.refresh(product);

            if (!product.isAvailable()) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE,
                        "상품이 판매 불가 상태입니다: " + product.getName());
            }

            product.deductStock(cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.updateTotalAmount(totalAmount);
        cartItemRepository.deleteAll(cartItems);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .data(OrderCreatedEvent.OrderCreatedData.builder()
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .memberId(memberId)
                        .totalAmount(totalAmount)
                        .itemCount(cartItems.size())
                        .items(order.getItems().stream().map(item -> Map.<String, Object>of(
                                "productId", item.getProduct().getId(),
                                "productName", item.getProductName(),
                                "quantity", item.getQuantity(),
                                "price", item.getProductPrice()
                        )).toList())
                        .build())
                .build();
        applicationEventPublisher.publishEvent(event);

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return paymentRepository.findByOrderId(orderId)
                .map(payment -> OrderResponse.from(order, payment))
                .orElseGet(() -> OrderResponse.from(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderListResponse> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderListResponse::from);
    }

    @Transactional
    public OrderCancelResponse cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus currentStatus = order.getStatus();
        order.changeStatus(OrderStatus.CANCELLED);

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            product.restoreStock(item.getQuantity());
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (currentStatus == OrderStatus.PAID) {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
            payment.cancel();
            refundAmount = payment.getAmount();
        }

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .data(OrderCancelledEvent.OrderCancelledData.builder()
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .reason("사용자 취소")
                        .refundAmount(refundAmount)
                        .build())
                .build();
        applicationEventPublisher.publishEvent(event);

        return OrderCancelResponse.from(order);
    }

    private String generateOrderNumber() {
        String datePrefix = "ORD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long seq = orderNumberSequenceRepository.save(new OrderNumberSequence()).getId();
        return datePrefix + String.format("%06d", seq);
    }
}
