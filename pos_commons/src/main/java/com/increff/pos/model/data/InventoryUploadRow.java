package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryUploadRow {
    private int rowNumber;
    private String barcode;
    private String quantity;
}
