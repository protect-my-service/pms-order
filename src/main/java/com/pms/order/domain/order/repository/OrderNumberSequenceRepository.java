package com.pms.order.domain.order.repository;

import com.pms.order.domain.order.entity.OrderNumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderNumberSequenceRepository extends JpaRepository<OrderNumberSequence, Long> {
}
