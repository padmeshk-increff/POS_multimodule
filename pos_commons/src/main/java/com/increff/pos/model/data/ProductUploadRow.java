package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUploadRow {
    private int rowNumber;
    private String barcode;
    private String name;
    private String mrp;
    private String clientName;
    private String category;
}
