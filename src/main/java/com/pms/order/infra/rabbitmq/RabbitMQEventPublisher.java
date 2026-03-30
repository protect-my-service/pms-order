package com.pms.order.infra.rabbitmq;

import com.pms.order.event.OrderCancelledEvent;
import com.pms.order.event.OrderCreatedEvent;
import com.pms.order.event.OrderEventPublisher;
import com.pms.order.event.OrderPaidEvent;
import com.pms.order.global.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQEventPublisher implements OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, event);
            log.info("Published OrderCreatedEvent: orderId={}", event.getData().getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent: orderId={}", event.getData().getOrderId(), e);
        }
    }

    @Override
    public void publishOrderPaid(OrderPaidEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_PAID_KEY, event);
            log.info("Published OrderPaidEvent: orderId={}", event.getData().getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish OrderPaidEvent: orderId={}", event.getData().getOrderId(), e);
        }
    }

    @Override
    public void publishOrderCancelled(OrderCancelledEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CANCELLED_KEY, event);
            log.info("Published OrderCancelledEvent: orderId={}", event.getData().getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish OrderCancelledEvent: orderId={}", event.getData().getOrderId(), e);
        }
    }
}
