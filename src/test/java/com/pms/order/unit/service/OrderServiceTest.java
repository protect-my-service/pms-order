package com.pms.order.unit.service;

import com.pms.order.domain.cart.entity.Cart;
import com.pms.order.domain.cart.entity.CartItem;
import com.pms.order.domain.cart.repository.CartItemRepository;
import com.pms.order.domain.cart.repository.CartRepository;
import com.pms.order.domain.member.entity.Member;
import com.pms.order.domain.member.repository.MemberRepository;
import com.pms.order.domain.order.dto.CreateOrderRequest;
import com.pms.order.domain.order.dto.OrderResponse;
import com.pms.order.domain.order.entity.Order;
import com.pms.order.domain.order.entity.OrderItem;
import com.pms.order.domain.order.entity.OrderStatus;
import com.pms.order.domain.order.entity.OrderNumberSequence;
import com.pms.order.domain.order.repository.OrderNumberSequenceRepository;
import com.pms.order.domain.order.repository.OrderRepository;
import com.pms.order.domain.order.service.OrderService;
import com.pms.order.domain.payment.entity.Payment;
import com.pms.order.domain.payment.repository.PaymentRepository;
import com.pms.order.domain.product.entity.Product;
import com.pms.order.domain.product.entity.ProductStatus;
import com.pms.order.domain.product.repository.ProductRepository;
import com.pms.order.global.exception.BusinessException;
import com.pms.order.global.exception.ErrorCode;
import com.pms.order.support.TestFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import com.pms.order.domain.order.dto.OrderCancelResponse;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import com.pms.order.event.OrderCreatedEvent;
import com.pms.order.event.OrderCancelledEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderNumberSequenceRepository orderNumberSequenceRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private EntityManager entityManager;

    private Member member;
    private Cart cart;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        member = TestFixture.member(1L, "user@test.com", "테스트유저");
        cart = TestFixture.cart(1L, member);
        productA = TestFixture.product(10L, "상품A", BigDecimal.valueOf(29900), 100, ProductStatus.ON_SALE);
        productB = TestFixture.product(20L, "상품B", BigDecimal.valueOf(15000), 50, ProductStatus.ON_SALE);
    }

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("장바구니 상품으로 주문을 생성하면 재고가 차감되고 주문 응답이 반환된다")
        void should_create_order_and_deduct_stock() {
            // given
            CartItem cartItem = TestFixture.cartItem(100L, cart, productA, 2);
            CreateOrderRequest request = new CreateOrderRequest();
            ReflectionTestUtils.setField(request, "cartItemIds", List.of(100L));

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
            given(cartItemRepository.findAllByIdInAndCartId(List.of(100L), 1L)).willReturn(List.of(cartItem));
            given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(productA));
            given(orderNumberSequenceRepository.save(any(OrderNumberSequence.class))).willAnswer(invocation -> {
                OrderNumberSequence seq = invocation.getArgument(0);
                ReflectionTestUtils.setField(seq, "id", 1L);
                return seq;
            });
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                ReflectionTestUtils.setField(order, "id", 1L);
                return order;
            });

            // when
            OrderResponse response = orderService.createOrder(1L, request);

            // then
            assertThat(response.getOrderNumber()).startsWith("ORD-");
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getItems()).hasSize(1);
            assertThat(productA.getStockQuantity()).isEqualTo(98);
            verify(cartItemRepository).deleteAll(List.of(cartItem));
            var eventCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(OrderCreatedEvent.class);
            assertThat(((OrderCreatedEvent) eventCaptor.getValue()).getData().getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 회원으로 주문하면 예외가 발생한다")
        void should_throw_when_member_not_found() {
            // given
            CreateOrderRequest request = new CreateOrderRequest();
            ReflectionTestUtils.setField(request, "cartItemIds", List.of(100L));
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("장바구니가 비어있으면 예외가 발생한다")
        void should_throw_when_cart_items_empty() {
            // given
            CreateOrderRequest request = new CreateOrderRequest();
            ReflectionTestUtils.setField(request, "cartItemIds", List.of(100L));

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
            given(cartItemRepository.findAllByIdInAndCartId(List.of(100L), 1L)).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.CART_EMPTY));
        }

        @Test
        @DisplayName("판매 불가 상품이 포함되면 예외가 발생한다")
        void should_throw_when_product_not_available() {
            // given
            Product soldOutProduct = TestFixture.product(10L, "품절상품", BigDecimal.valueOf(10000), 0, ProductStatus.SOLD_OUT);
            CartItem cartItem = TestFixture.cartItem(100L, cart, productA, 1);
            CreateOrderRequest request = new CreateOrderRequest();
            ReflectionTestUtils.setField(request, "cartItemIds", List.of(100L));

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
            given(cartItemRepository.findAllByIdInAndCartId(List.of(100L), 1L)).willReturn(List.of(cartItem));
            given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(soldOutProduct));
            given(orderNumberSequenceRepository.save(any(OrderNumberSequence.class))).willAnswer(invocation -> {
                OrderNumberSequence seq = invocation.getArgument(0);
                ReflectionTestUtils.setField(seq, "id", 1L);
                return seq;
            });
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                ReflectionTestUtils.setField(order, "id", 1L);
                return order;
            });

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PRODUCT_NOT_AVAILABLE));
        }

        @Test
        @DisplayName("재고가 부족하면 예외가 발생한다")
        void should_throw_when_insufficient_stock() {
            // given
            Product lowStockProduct = TestFixture.product(10L, "재고부족", BigDecimal.valueOf(10000), 1, ProductStatus.ON_SALE);
            CartItem cartItem = TestFixture.cartItem(100L, cart, lowStockProduct, 5);
            CreateOrderRequest request = new CreateOrderRequest();
            ReflectionTestUtils.setField(request, "cartItemIds", List.of(100L));

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
            given(cartItemRepository.findAllByIdInAndCartId(List.of(100L), 1L)).willReturn(List.of(cartItem));
            given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(lowStockProduct));
            given(orderNumberSequenceRepository.save(any(OrderNumberSequence.class))).willAnswer(invocation -> {
                OrderNumberSequence seq = invocation.getArgument(0);
                ReflectionTestUtils.setField(seq, "id", 1L);
                return seq;
            });
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                ReflectionTestUtils.setField(order, "id", 1L);
                return order;
            });

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
        }

        @Test
        @DisplayName("일부 장바구니 상품이 존재하지 않으면 예외가 발생한다")
        void should_throw_when_some_cart_items_not_found() {
            // given
            CartItem cartItem = TestFixture.cartItem(100L, cart, productA, 1);
            CreateOrderRequest request = new CreateOrderRequest();
            ReflectionTestUtils.setField(request, "cartItemIds", List.of(100L, 200L));

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
            given(cartItemRepository.findAllByIdInAndCartId(List.of(100L, 200L), 1L)).willReturn(List.of(cartItem));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelOrder {

        @Test
        @DisplayName("PENDING 상태의 주문을 취소하면 재고가 복원된다")
        void should_restore_stock_when_cancelling_pending_order() {
            // given
            Order order = TestFixture.order(1L, "ORD-20250401-000001", member, OrderStatus.PENDING, BigDecimal.valueOf(59800));
            OrderItem orderItem = TestFixture.orderItem(1L, order, productA, 2);
            order.addItem(orderItem);

            given(orderRepository.findByIdAndMemberId(1L, 1L)).willReturn(Optional.of(order));
            given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(productA));

            int stockBefore = productA.getStockQuantity();

            // when
            OrderCancelResponse result = orderService.cancelOrder(1L, 1L);

            // then
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(productA.getStockQuantity()).isEqualTo(stockBefore + 2);
            var eventCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(OrderCancelledEvent.class);
            assertThat(((OrderCancelledEvent) eventCaptor.getValue()).getData().getOrderNumber())
                    .isEqualTo("ORD-20250401-000001");
        }

        @Test
        @DisplayName("PAID 상태의 주문을 취소하면 결제도 취소된다")
        void should_cancel_payment_when_cancelling_paid_order() {
            // given
            Order order = TestFixture.order(1L, "ORD-20250401-000001", member, OrderStatus.PAID, BigDecimal.valueOf(29900));
            OrderItem orderItem = TestFixture.orderItem(1L, order, productA, 1);
            order.addItem(orderItem);
            Payment payment = TestFixture.payment(1L, order, "PAY-test-key", BigDecimal.valueOf(29900));
            payment.approve();

            given(orderRepository.findByIdAndMemberId(1L, 1L)).willReturn(Optional.of(order));
            given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(productA));
            given(paymentRepository.findByOrderId(1L)).willReturn(Optional.of(payment));

            // when
            OrderCancelResponse result = orderService.cancelOrder(1L, 1L);

            // then
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(payment.getStatus().name()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("존재하지 않는 주문을 취소하면 예외가 발생한다")
        void should_throw_when_order_not_found() {
            // given
            given(orderRepository.findByIdAndMemberId(999L, 1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("주문번호 생성")
    class GenerateOrderNumber {

        @Test
        @DisplayName("주문번호는 DB에서 발급한 시퀀스 ID로 생성된다")
        void should_generate_order_number_using_db_sequence() {
            // given
            CartItem cartItem = TestFixture.cartItem(100L, cart, productA, 1);
            CreateOrderRequest request = new CreateOrderRequest();
            ReflectionTestUtils.setField(request, "cartItemIds", List.of(100L));

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
            given(cartItemRepository.findAllByIdInAndCartId(List.of(100L), 1L)).willReturn(List.of(cartItem));
            given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(productA));
            given(orderNumberSequenceRepository.save(any(OrderNumberSequence.class))).willAnswer(invocation -> {
                OrderNumberSequence seq = invocation.getArgument(0);
                ReflectionTestUtils.setField(seq, "id", 42L);
                return seq;
            });
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                ReflectionTestUtils.setField(order, "id", 1L);
                return order;
            });

            // when
            OrderResponse response = orderService.createOrder(1L, request);

            // then — DB가 발급한 id(42)가 시퀀스로 사용된다
            assertThat(response.getOrderNumber()).endsWith("-000042");
            verify(orderNumberSequenceRepository).save(any(OrderNumberSequence.class));
        }
    }
}
