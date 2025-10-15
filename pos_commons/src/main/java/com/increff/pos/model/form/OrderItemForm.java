package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
public class OrderItemForm {

    @NotNull(message = "ProductId cannot be null")
    private Integer productId;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be a positive number")
    private Integer quantity;

    @NotNull(message = "Selling price cannot be null")
    @Positive(message = "Selling price must be a positive number")
    private Double sellingPrice;

}