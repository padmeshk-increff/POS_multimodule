package com.increff.pos.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InventoryReportResult {
    private final Integer productId;
    private final String productName;
    private final String barcode; // Included as it's selected in the DAO query
    private final String category; // Included as it's selected in the DAO query
    private final Double mrp;      // Included as it's selected in the DAO query
    private final Integer quantity;
}

