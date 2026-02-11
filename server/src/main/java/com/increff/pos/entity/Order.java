package com.increff.pos.entity;

import com.increff.pos.model.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(
    indexes = {
        @Index(name = "idx_order_created_at", columnList = "createdAt"),
        @Index(name = "idx_order_status", columnList = "orderStatus"),
        @Index(name = "idx_order_total_amount", columnList = "totalAmount")
    }
)
public class Order extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    private String customerName;

    private String customerPhone;

    private String invoicePath;

    @Column(nullable = false)
    private Double totalAmount;

}