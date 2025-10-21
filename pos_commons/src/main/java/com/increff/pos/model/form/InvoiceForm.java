package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class InvoiceForm {
    private Integer orderId;
    private ZonedDateTime orderDate;
    private String customerName;
    private String customerPhone;
    private Double totalAmount;
    private List<InvoiceItemForm> items;
}
