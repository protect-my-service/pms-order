package com.pms.order.event;

public interface OrderEventPublisher {

    void publishOrderCreated(OrderCreatedEvent event);

    void publishOrderPaid(OrderPaidEvent event);

    void publishOrderCancelled(OrderCancelledEvent event);
}
