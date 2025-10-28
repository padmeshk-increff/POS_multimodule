package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.InventoryReportData;
import com.increff.pos.model.data.SalesReportData;
import com.increff.pos.model.result.InventoryReportResult;
import com.increff.pos.model.result.ProductQuantityResult;
import com.increff.pos.model.data.SummaryData;
import com.increff.pos.model.data.SummaryData.*;
import com.increff.pos.model.result.SalesOverTimeResult;
import com.increff.pos.utils.ReportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = ApiException.class)
public class ReportFlow {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int TOP_PRODUCTS_LIMIT = 5;

    @Autowired private OrderApi orderApi;
    @Autowired private OrderItemApi orderItemApi;
    @Autowired private ProductApi productApi;
    @Autowired private InventoryApi inventoryApi;

    public SummaryData getSummaryData() throws ApiException {

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime today = ZonedDateTime.now(zone);
        ZonedDateTime todayStart = today.toLocalDate().atStartOfDay(zone);
        ZonedDateTime yesterdayStart = todayStart.minusDays(1);

        List<Order> todayOrders = orderApi.getAllByDateRange(todayStart, todayStart.plusDays(1));
        List<Order> yesterdayOrders = orderApi.getAllByDateRange(yesterdayStart, todayStart);

        KpiData todaySales = ReportUtil.calculateSalesKpi(todayOrders, yesterdayOrders);
        KpiData todayOrdersKpi = ReportUtil.calculateOrdersKpi(todayOrders, yesterdayOrders);
        KpiData avgOrderValue = ReportUtil.calculateAovKpi(todayOrders, yesterdayOrders);

        List<SalesByHourData> salesByHour = ReportUtil.getSalesByHour(todayOrders);
        List<ProductSalesData> topProducts = getTopSellingProducts(todayStart,todayStart.plusDays(1),TOP_PRODUCTS_LIMIT);
        List<LowStockAlertData> lowStockAlerts = getLowStockAlerts();

        return new SummaryData(
                todaySales,
                todayOrdersKpi,
                avgOrderValue,
                salesByHour,
                topProducts,
                lowStockAlerts
        );
    }

    public SalesReportData getSalesReport(ZonedDateTime start, ZonedDateTime end) throws ApiException {
        List<Order> orders = orderApi.getAllByDateRange(start, end);

        List<SalesOverTimeResult> salesByDay = orderItemApi.getSalesByDate(start, end);

        List<SalesReportData.SalesOverTimeData> salesOverTimeData = ReportUtil.convert(salesByDay);

        List<ProductSalesData> productPerformance = getTopSellingProducts(start, end, null);

        SalesReportData.SalesSummaryData summary = ReportUtil.calculateSalesSummary(start, end, orders, productPerformance);

        return new SalesReportData(summary, salesOverTimeData, productPerformance);
    }

    public InventoryReportData getInventoryReport(){
        List<InventoryReportResult> reportResults = inventoryApi.getInventoryReportData();

        List<InventoryReportData.InventoryItemData> items = ReportUtil.convert(reportResults,LOW_STOCK_THRESHOLD);

        InventoryReportData.InventorySummaryData summary = ReportUtil.calculateInventorySummary(items,LOW_STOCK_THRESHOLD);

        return new InventoryReportData(summary, items);
    }

    private List<ProductSalesData> getTopSellingProducts(ZonedDateTime start,ZonedDateTime end,Integer threshold) throws ApiException {
        List<ProductQuantityResult> topProductStats = orderItemApi.getTopSellingProducts(start,end, threshold);

        if (topProductStats.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> productIds = ReportUtil.getProductIds(topProductStats,ProductQuantityResult::getProductId);

        List<Product> products = productApi.getByIds(productIds);
        Map<Integer, String> productMap = ReportUtil.mapIdToName(products);

        return ReportUtil.buildProductSalesData(topProductStats,productMap);
    }

    private List<LowStockAlertData> getLowStockAlerts() throws ApiException {
        List<Inventory> lowStockItems = inventoryApi.getLowStockItems(LOW_STOCK_THRESHOLD);

        if (lowStockItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> productIds = ReportUtil.getProductIds(lowStockItems,Inventory::getProductId);

        List<Product> products = productApi.getByIds(productIds);
        Map<Integer, String> productMap = ReportUtil.mapIdToName(products);

        return ReportUtil.buildLowStockAlertData(lowStockItems,productMap);
    }
}

