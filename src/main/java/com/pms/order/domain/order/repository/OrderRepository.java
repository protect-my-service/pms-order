package com.pms.order.domain.order.repository;

import com.pms.order.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :orderId AND o.member.id = :memberId")
    Optional<Order> findByIdAndMemberId(@Param("orderId") Long orderId, @Param("memberId") Long memberId);

    Page<Order> findByMemberId(Long memberId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderNumber LIKE :prefix%")
    long countByOrderNumberPrefix(@Param("prefix") String prefix);

}
