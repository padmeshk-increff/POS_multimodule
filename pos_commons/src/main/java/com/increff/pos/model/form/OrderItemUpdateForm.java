package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
public class OrderItemUpdateForm {

    @NotNull(message = "Quantity must not be nul")
    @Positive(message = "Quantity must be a positive number")
    private Integer quantity;

    @NotNull(message = "Selling Price should not be null")
    @Positive(message = "Selling price must be a positive number")
    private Double sellingPrice;

}
