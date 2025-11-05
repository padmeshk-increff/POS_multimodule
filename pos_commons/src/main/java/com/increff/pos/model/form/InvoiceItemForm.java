package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

// Import the validation annotations
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Getter
@Setter
public class InvoiceItemForm {

    @NotBlank(message = "Product name cannot be null or blank")
    private String productName;

    @NotBlank(message = "Barcode cannot be null or blank")
    private String barcode;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be a positive number (at least 1)")
    private Integer quantity;

    @NotNull(message = "MRP cannot be null")
    @PositiveOrZero(message = "MRP must be zero or positive")
    private Double mrp;

    @NotNull(message = "Selling price cannot be null")
    @PositiveOrZero(message = "Selling price must be zero or positive")
    private Double sellingPrice;
}