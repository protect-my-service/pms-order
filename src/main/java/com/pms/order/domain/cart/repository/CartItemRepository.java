package com.pms.order.domain.cart.repository;

import com.pms.order.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.id IN :ids AND ci.cart.id = :cartId")
    List<CartItem> findAllByIdInAndCartId(@Param("ids") List<Long> ids, @Param("cartId") Long cartId);
}
