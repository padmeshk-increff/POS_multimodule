package com.increff.pos.model.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class SummaryData {

    /**
     * KPI Card: Total revenue for today vs. yesterday.
     */
    private final KpiData todaySales;

    /**
     * KPI Card: Total number of orders for today vs. yesterday.
     */
    private final KpiData todayOrders;

    /**
     * KPI Card: Average order value for today vs. yesterday.
     */
    private final KpiData averageOrderValue;

    /**
     * Data for the "Today's Sales by Hour" bar/line chart.
     */
    private final List<SalesByHourData> salesByHour;

    /**
     * Data for the "Top 5 Selling Products" widget.
     */
    private final List<ProductSalesData> topSellingProducts;

    /**
     * Data for the "Low Stock Alerts" widget.
     */
    private final List<LowStockAlertData> lowStockAlerts;


    /**
     * A reusable structure for a single KPI (Key Performance Indicator) card.
     */
    @Getter
    @AllArgsConstructor
    public static class KpiData {

        private final double current;
        private final double previous;
        private final double changePercent;
    }

    /**
     * Represents a single data point for the hourly sales chart.
     */
    @Getter
    @AllArgsConstructor
    public static class SalesByHourData {

        private final int hour;
        private final double revenue;
    }

    /**
     * Represents a single product in the "Top Selling Products" list.
     * 'totalRevenue' will be null for the dashboard summary.
     */
    @Getter
    @AllArgsConstructor
    public static class ProductSalesData {

        private final Integer productId;
        private final String productName;
        private final long quantitySold;
        private final Double totalRevenue;
    }

    /**
     B* Represents a single product in the "Low Stock Alerts" list.
     */
    @Getter
    @AllArgsConstructor
    public static class LowStockAlertData {

        private final Integer productId;
        private final String productName;
        private final int currentStock;
    }
}
