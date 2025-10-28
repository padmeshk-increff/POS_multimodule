package com.increff.pos.model.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime; // For report generation timestamp
import java.util.List;

@Getter
@AllArgsConstructor
public class InventoryReportData {

    private final InventorySummaryData summary;
    private final List<InventoryItemData> items;

    @Getter
    @AllArgsConstructor
    public static class InventorySummaryData {
        private final ZonedDateTime reportGeneratedAt; // Timestamp when the report was created
        private final Long totalProductSkus;         // Total number of distinct products
        private final Long totalInventoryQuantity;   // Sum of quantities of all products
        private final Double totalInventoryValue;    // Sum of (mrp * quantity) for all products
        private final Long outOfStockItems;          // Count of items with quantity <= 0
        private final Long lowStockItems;            // Count of items with quantity > 0 and < threshold
    }

    @Getter
    @AllArgsConstructor
    public static class InventoryItemData {
        private final Integer productId;
        private final String productName;
        private final Integer quantity;
        private final Double totalValue; // Calculated: mrp * quantity
        private final String status;     // e.g., "In Stock", "Low Stock", "Out of Stock"
    }
}

