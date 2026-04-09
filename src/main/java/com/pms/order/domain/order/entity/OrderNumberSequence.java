package com.pms.order.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_number_sequence")
@Getter
@NoArgsConstructor
public class OrderNumberSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
