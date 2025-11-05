package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

// Import the validation annotations
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class InvoiceForm {

    @NotNull(message = "Order ID cannot be null")
    private Integer orderId;

    @NotNull(message = "Order date cannot be null")
    private ZonedDateTime orderDate;

    private String customerName;

    private String customerPhone;

    @NotNull(message = "Total amount cannot be null")
    @PositiveOrZero(message = "Total amount must be zero or positive")
    private Double totalAmount;

    @NotNull(message = "Item list cannot be null")
    @NotEmpty(message = "Invoice must have at least one item")
    @Valid // This tells the validator to also check the InvoiceItemForm objects *inside* the list
    private List<InvoiceItemForm> items;
}