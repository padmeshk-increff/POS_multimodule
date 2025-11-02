package com.increff.pos.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductQuantityResult{

    private Integer productId;
    private Long totalQuantity;
    private Double totalRevenue;

}
