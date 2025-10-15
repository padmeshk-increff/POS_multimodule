package com.increff.pos.model.result;

import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderResult {

    private Order order;
    private List<OrderItem> orderItems;

}
