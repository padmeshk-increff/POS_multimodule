package com.increff.pos.model.form;

import com.increff.pos.model.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class OrderUpdateForm {
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;

    @Size(max = 20, message = "Customer phone must not exceed 20 characters")
    @Pattern(regexp = "^[0-9]*$", message = "Phone number must contain only digits")
    private String customerPhone;

    @NotNull(message = "Order status cannot be null")
    private OrderStatus status;
}
