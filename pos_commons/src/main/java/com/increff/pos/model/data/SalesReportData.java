package com.increff.pos.model.data;

import com.increff.pos.model.data.SummaryData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime; // CHANGED
import java.util.List;


// --- DTO for Sales Report ---
@Getter
@AllArgsConstructor
public class SalesReportData {

    private final SalesSummaryData summary;
    private final List<SalesOverTimeData> salesOverTime;
    // We can reuse the ProductSalesData from the SummaryData DTO
    private final List<SummaryData.ProductSalesData> productPerformance;

    @Getter
    @AllArgsConstructor
    public static class SalesSummaryData {
        private final ZonedDateTime startDate; // CHANGED
        private final ZonedDateTime endDate;   // CHANGED
        private final double totalRevenue;
        private final long totalOrders;
        private final double averageOrderValue;
        private final long totalItemsSold;
    }

    @Getter
    @AllArgsConstructor
    public static class SalesOverTimeData {
        private final ZonedDateTime date; // CHANGED
        private final double revenue;
    }
}

