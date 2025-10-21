package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceItemForm {
    private String productName;
    private String barcode;
    private Integer quantity;
    private Double mrp;
    private Double sellingPrice;
}
