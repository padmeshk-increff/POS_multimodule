package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Getter
@Setter
public class OrderForm {

    @Size(max = 100, message = "Customer name cannot exceed 100 characters")
    private String customerName;

    @Size(max = 20, message = "Customer phone cannot exceed 20 characters")
    @Pattern(regexp = "^[0-9]*$", message = "Phone number must contain only digits")
    private String customerPhone;

    @NotNull(message = "Items list cannot be null")
    @NotEmpty(message = "Order must contain at least one item")
    @Valid   // Ensures each item in the list is validated
    private List<OrderItemForm> items;
}
