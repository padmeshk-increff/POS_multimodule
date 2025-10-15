package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemData {

    private Integer id;
    private Integer quantity;
    private Double sellingPrice;
    private String productName;

}
