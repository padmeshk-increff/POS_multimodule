package com.increff.pos.model.data;

import com.increff.pos.model.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class OrderData{

    private Integer id;
    private OrderStatus orderStatus;
    private String customerName;
    private String customerPhone;
    private Double totalAmount;
    private List<OrderItemData> orderItemDataList;
    private ZonedDateTime createdAt;
}
