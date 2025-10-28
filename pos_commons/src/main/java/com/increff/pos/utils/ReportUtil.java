package com.increff.pos.utils;

import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.InventoryReportData;
import com.increff.pos.model.data.SalesReportData;
import com.increff.pos.model.data.SummaryData;
import com.increff.pos.model.data.SummaryData.*;
import com.increff.pos.model.result.InventoryReportResult;
import com.increff.pos.model.result.ProductQuantityResult;
import com.increff.pos.model.result.SalesOverTimeResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportUtil {

    public static List<SalesReportData.SalesOverTimeData> convert(List<SalesOverTimeResult> salesByDay){
        ZoneId zone = ZoneId.systemDefault();
        return salesByDay.stream()
                .map(s -> {
                    ZonedDateTime zdt = Instant.ofEpochMilli(s.getDate().getTime())
                            .atZone(zone)
                            .toLocalDate()
                            .atStartOfDay(zone);
                    return new SalesReportData.SalesOverTimeData(zdt, s.getRevenue());
                })
                .collect(Collectors.toList());
    }

    public static List<InventoryReportData.InventoryItemData> convert(List<InventoryReportResult> reportResults,Integer LOW_STOCK_THRESHOLD){
        return reportResults.stream()
                .map(result -> buildInventoryItemData(result,LOW_STOCK_THRESHOLD))
                .collect(Collectors.toList());
    }

    public static InventoryReportData.InventorySummaryData calculateInventorySummary(List<InventoryReportData.InventoryItemData> items, Integer LOW_STOCK_THRESHOLD) {
        long totalSkus = items.size();
        long totalQuantity = items.stream()
                .mapToLong(InventoryReportData.InventoryItemData::getQuantity)
                .sum();
        double totalValue = items.stream()
                .mapToDouble(InventoryReportData.InventoryItemData::getTotalValue)
                .sum();
        long outOfStock = items.stream()
                .filter(item -> item.getQuantity() <= 0)
                .count();
        long lowStock = items.stream()
                .filter(item -> item.getQuantity() > 0 && item.getQuantity() < LOW_STOCK_THRESHOLD)
                .count();

        totalValue = Math.round(totalValue * 100.0) / 100.0;

        return new InventoryReportData.InventorySummaryData(
                ZonedDateTime.now(ZoneId.systemDefault()),
                totalSkus,
                totalQuantity,
                totalValue,
                outOfStock,
                lowStock
        );
    }

    public static SalesReportData.SalesSummaryData calculateSalesSummary(ZonedDateTime start, ZonedDateTime end, List<Order> orders, List<ProductSalesData> products) {
        double totalRevenue = orders.stream().mapToDouble(Order::getTotalAmount).sum();
        long totalOrders = orders.size();
        double avgOrderValue = (totalOrders == 0) ? 0.0 : totalRevenue / totalOrders;
        long totalItemsSold = products.stream().mapToLong(ProductSalesData::getQuantitySold).sum();

        totalRevenue = Math.round(totalRevenue * 100.0) / 100.0;
        avgOrderValue = Math.round(avgOrderValue * 100.0) / 100.0;

        return new SalesReportData.SalesSummaryData(
                start,
                end,
                totalRevenue,
                totalOrders,
                avgOrderValue,
                totalItemsSold
        );
    }

    public static SummaryData.KpiData calculateSalesKpi(List<Order> today, List<Order> yesterday) {
        double current = today.stream().mapToDouble(Order::getTotalAmount).sum();
        double previous = yesterday.stream().mapToDouble(Order::getTotalAmount).sum();
        return buildKpi(current, previous);
    }

    public static SummaryData.KpiData calculateOrdersKpi(List<Order> today, List<Order> yesterday) {
        double current = today.size();
        double previous = yesterday.size();
        return buildKpi(current, previous);
    }

    public static SummaryData.KpiData calculateAovKpi(List<Order> today, List<Order> yesterday) {
        double currentSales = today.stream().mapToDouble(Order::getTotalAmount).sum();
        double previousSales = yesterday.stream().mapToDouble(Order::getTotalAmount).sum();

        double currentAov = (today.isEmpty()) ? 0.0 : currentSales / today.size();
        double previousAov = (yesterday.isEmpty()) ? 0.0 : previousSales / yesterday.size();

        return buildKpi(currentAov, previousAov);
    }

    public static List<SummaryData.SalesByHourData> getSalesByHour(List<Order> todayOrders) {
        if (todayOrders.isEmpty()) {
            return Collections.emptyList();
        }
        return todayOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().getHour(),
                        Collectors.summingDouble(Order::getTotalAmount)
                ))
                .entrySet().stream()
                .map(entry -> new SummaryData.SalesByHourData(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(a.getHour(), b.getHour()))
                .collect(Collectors.toList());
    }

    public static <T> List<Integer> getProductIds(Collection<T> items, Function<T, Integer> idExtractor) {
        if (items == null || items.isEmpty() || idExtractor == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)      // Avoid NPE if an item in the collection is null
                .map(idExtractor)             // Apply the function to get the ID
                .filter(Objects::nonNull)      // Avoid NPE if the extracted ID is null
                // .distinct() // Uncomment this if you need unique IDs only
                .collect(Collectors.toList()); // Collect the IDs into a list
    }

    public static Map<Integer,String> mapIdToName(List<Product> products){
        return products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
    }

    public static List<ProductSalesData> buildProductSalesData(List<ProductQuantityResult> topProductStats,Map<Integer,String> productMap){
        return topProductStats.stream()
                .map(stats -> {
                    String productName = productMap.getOrDefault(stats.getProductId(), "Unknown Product");
                    return new ProductSalesData(
                            stats.getProductId(),
                            productName,
                            stats.getTotalQuantity(),
                            stats.getTotalRevenue()
                    );
                })
                .collect(Collectors.toList());
    }

    public static List<LowStockAlertData> buildLowStockAlertData(List<Inventory> lowStockItems,Map<Integer,String> productMap){
        return lowStockItems.stream()
                .map(inventory -> {
                    String productName = productMap.getOrDefault(inventory.getProductId(), "Unknown Product");
                    return new LowStockAlertData(
                            inventory.getProductId(),
                            productName,
                            inventory.getQuantity()
                    );
                })
                .collect(Collectors.toList());
    }

    private static SummaryData.KpiData buildKpi(double current, double previous) {
        double changePercent = 0.0;
        if (previous > 0) {
            changePercent = ((current - previous) / previous) * 100.0;
        } else if (current > 0) {
            changePercent = 100.0;
        }
        changePercent = Math.round(changePercent * 100.0) / 100.0;
        double roundedCurrent = Math.round(current * 100.0) / 100.0;
        double roundedPrevious = Math.round(previous * 100.0) / 100.0;
        return new SummaryData.KpiData(roundedCurrent, roundedPrevious, changePercent);
    }

    private static InventoryReportData.InventoryItemData buildInventoryItemData(InventoryReportResult result,Integer LOW_STOCK_THRESHOLD) {
        double totalValue = 0.0;
        if (result.getMrp() != null && result.getQuantity() != null) {
            totalValue = result.getMrp() * result.getQuantity();
            totalValue = Math.round(totalValue * 100.0) / 100.0; // Round to 2 decimals
        }

        String status;
        if (result.getQuantity() == null || result.getQuantity() <= 0) {
            status = "Out of Stock";
        } else if (result.getQuantity() < LOW_STOCK_THRESHOLD) {
            status = "Low Stock";
        } else {
            status = "In Stock";
        }

        return new InventoryReportData.InventoryItemData(
                result.getProductId(),
                result.getProductName(),
                result.getQuantity(),
                totalValue,
                status
        );
    }
}
