package com.increff.pos.integration.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.config.SpringConfig;
import com.increff.pos.dto.ReportDto;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.factory.ClientFactory;
import com.increff.pos.factory.InventoryFactory;
import com.increff.pos.factory.OrderFactory;
import com.increff.pos.factory.OrderItemFactory;
import com.increff.pos.factory.ProductFactory;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.SummaryData;
import com.increff.pos.model.result.OrderResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration Tests for the ReportDto class.
 * This test validates the data aggregation and TSV generation logic.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@WebAppConfiguration
@TestPropertySource("classpath:test.properties")
@Transactional
public class ReportDtoTest {

    @Autowired
    private ReportDto reportDto; // Class under test

    // --- Setup Dependencies ---
    @Autowired
    private ClientApi clientApi;
    @Autowired
    private ProductFlow productFlow;
    @Autowired
    private InventoryApi inventoryApi;
    @Autowired
    private OrderFlow orderFlow;
    @Autowired
    private OrderApi orderApi; // To invoice orders

    // --- Prerequisite Data ---
    private Client testClient;
    private Product product1, product2, product3;

    // --- Expected Aggregates ---
    private Double expectedRevenue;
    private Integer expectedItemsSold;
    private Integer expectedInvoicedOrders;
    private Integer expectedTotalStock;

    /**
     * Helper method to create a test Client ENTITY using the ClientApi.
     */
    private Client createTestClient(String name) throws ApiException {
        Client client = ClientFactory.mockNewObject(name);
        return clientApi.insert(client);
    }

    /**
     * Helper method to create a Product ENTITY and set its Inventory.
     */
    private Product createTestProduct(Integer clientId, String barcode, String name, Double mrp, Integer inventory) throws ApiException {
        Product product = ProductFactory.mockNewObject(barcode, clientId);
        product.setName(name);
        product.setMrp(mrp);
        product.setCategory("test-category");

        Product insertedProduct = productFlow.insert(product);

        Inventory inventoryUpdate = InventoryFactory.mockNewObject(insertedProduct.getId());
        inventoryUpdate.setQuantity(inventory);

        inventoryApi.updateByProductId(insertedProduct.getId(), inventoryUpdate);

        return insertedProduct;
    }

    /**
     * Creates a complex data state for testing reports:
     * - 3 Products with initial stock
     * - 2 INVOICED orders (which count towards sales)
     * - 1 CREATED order (which should NOT count towards sales)
     */
    @Before
    public void setUp() throws ApiException {
        this.testClient = createTestClient("test client");

        // Product 1: 100 in stock, MRP 100.0
        this.product1 = createTestProduct(testClient.getId(), "barcode-001", "product a", 100.0, 100);
        // Product 2: 100 in stock, MRP 50.0
        this.product2 = createTestProduct(testClient.getId(), "barcode-002", "product b", 50.0, 100);
        // Product 3: 100 in stock, MRP 20.0 (will not be sold, just for inventory)
        this.product3 = createTestProduct(testClient.getId(), "barcode-003", "product c", 20.0, 100);

        // --- Order 1 (INVOICED) ---
        Order order1 = OrderFactory.mockNewObject();
        // 10 of Product A @ 90.0 (Value: 900.0)
        OrderItem oi1_1 = OrderItemFactory.mockNewObject(null, product1.getId(), 10, 90.0);
        // 5 of Product B @ 50.0 (Value: 250.0)
        OrderItem oi1_2 = OrderItemFactory.mockNewObject(null, product2.getId(), 5, 50.0);
        OrderResult result1 = orderFlow.insert(order1, Arrays.asList(oi1_1, oi1_2));
        orderApi.updateInvoiceOrder(result1.getOrder().getId()); // Invoice it
        // Stock: p1=90, p2=95

        // --- Order 2 (INVOICED) ---
        Order order2 = OrderFactory.mockNewObject();
        // 20 of Product A @ 80.0 (Value: 1600.0)
        OrderItem oi2_1 = OrderItemFactory.mockNewObject(null, product1.getId(), 20, 80.0);
        OrderResult result2 = orderFlow.insert(order2, Arrays.asList(oi2_1));
        orderApi.updateInvoiceOrder(result2.getOrder().getId()); // Invoice it
        // Stock: p1=70

        // --- Order 3 (CREATED - Not Invoiced) ---
        Order order3 = OrderFactory.mockNewObject();
        // 1 of Product B @ 50.0 (Value: 50.0)
        OrderItem oi3_1 = OrderItemFactory.mockNewObject(null, product2.getId(), 1, 50.0);
        orderFlow.insert(order3, Arrays.asList(oi3_1));
        // Stock: p2=94

        // --- Set Expected Values for Tests ---
        // All "Today" calculations are based on the 2 INVOICED orders
        this.expectedRevenue = (10 * 90.0) + (5 * 50.0) + (20 * 80.0); // 900 + 250 + 1600 = 2750.0
        this.expectedItemsSold = 10 + 5 + 20; // 35
        this.expectedInvoicedOrders = 2;

        // p1=70, p2=94, p3=100
        this.expectedTotalStock = 70 + 94 + 100; // 264
    }

    // --- getSummary() Tests ---

    @Test
    public void getSummaryShouldReturnCorrectAggregates() throws ApiException {
        // GIVEN
        // Data is setup in @Before.
        // Expected Today's Sales: 2750.0 (from 2 invoiced orders)
        // Expected Today's Orders: 2
        // Expected Avg Order Value: 2750.0 / 2 = 1375.0
        // Expected Top Sellers: p1 (30), p2 (5)
        // Expected Low Stock: (Assuming < 10 threshold) p1=70, p2=94, p3=100 -> None are low.

        // WHEN
        SummaryData summary = reportDto.getSummary();

        // THEN
        assertNotNull(summary);

        // 1. Check KPI Cards (current values only, as we didn't create "previous" data)
        assertNotNull(summary.getTodaySales());
        assertEquals(expectedRevenue, summary.getTodaySales().getCurrent(), 0.01);

        assertNotNull(summary.getTodayOrders());
        assertEquals(expectedInvoicedOrders.doubleValue(), summary.getTodayOrders().getCurrent(), 0.01);

        assertNotNull(summary.getAverageOrderValue());
        Double expectedAvg = expectedRevenue / expectedInvoicedOrders;
        assertEquals(expectedAvg, summary.getAverageOrderValue().getCurrent(), 0.01);

        // 2. Check Today's Sales by Hour
        assertNotNull(summary.getSalesByHour());
        // All orders were created "now", so all revenue should be in the current hour
        double totalHourlyRevenue = summary.getSalesByHour().stream().mapToDouble(SummaryData.SalesByHourData::getRevenue).sum();
        assertEquals(expectedRevenue, totalHourlyRevenue, 0.01);

        // Check if the current hour has the revenue
        int currentHour = ZonedDateTime.now().getHour();
        double revenueInCurrentHour = summary.getSalesByHour().stream()
                .filter(s -> s.getHour() == currentHour)
                .mapToDouble(SummaryData.SalesByHourData::getRevenue)
                .sum();
        assertEquals(expectedRevenue, revenueInCurrentHour, 0.01);

        // 3. Check Top 5 Selling Products
        assertNotNull(summary.getTopSellingProducts());
        List<SummaryData.ProductSalesData> topProducts = summary.getTopSellingProducts();
        assertEquals(2, topProducts.size()); // Only 2 products were sold

        // p1 (Product A) should be first with 30 items
        assertEquals(product1.getId(), topProducts.get(0).getProductId());
        assertEquals(product1.getName().toLowerCase(), topProducts.get(0).getProductName());
        assertEquals(30, topProducts.get(0).getQuantitySold());

        // p2 (Product B) should be second with 5 items
        assertEquals(product2.getId(), topProducts.get(1).getProductId());
        assertEquals(product2.getName().toLowerCase(), topProducts.get(1).getProductName());
        assertEquals(5, topProducts.get(1).getQuantitySold());

        // 4. Check Low Stock Alerts
        // Assuming default threshold is < 10, stocks are 70, 94, 100.
        assertNotNull(summary.getLowStockAlerts());
        assertEquals("Low stock alerts should be empty", 0, summary.getLowStockAlerts().size());

        // --- Test the low stock alert by updating an item to be low ---
        // Set p1 stock to 5 (which is < 10)
        Inventory inv = inventoryApi.getCheckByProductId(product1.getId());
        inv.setQuantity(5);
        inventoryApi.update(inv);

        // Rerun the DTO method
        summary = reportDto.getSummary();

        // Now, the low stock list should have 1 item
        assertNotNull(summary.getLowStockAlerts());
        assertEquals("Low stock alerts should have 1 item", 1, summary.getLowStockAlerts().size());
        assertEquals(product1.getId(), summary.getLowStockAlerts().get(0).getProductId());
        assertEquals(product1.getName().toLowerCase(), summary.getLowStockAlerts().get(0).getProductName());
        assertEquals(5, summary.getLowStockAlerts().get(0).getCurrentStock());
    }

    // --- getSalesReport() Tests ---

    @Test
    public void getSalesReportShouldReturnValidTsv() throws ApiException {
        // GIVEN
        ZonedDateTime start = ZonedDateTime.now().minusDays(1);
        ZonedDateTime end = ZonedDateTime.now().plusDays(1);

        // WHEN
        ResponseEntity<byte[]> response = reportDto.getSalesReport(start, end);

        // THEN
        // 1. Check response headers
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getHeaders().containsKey("Content-Disposition"));
        String header = response.getHeaders().get("Content-Disposition").get(0);
        System.out.println("ACTUAL HEADER: [" + header + "]");
        assertTrue(header.startsWith("attachment; filename=\"sales-report-"));
        assertTrue(header.endsWith(".tsv\""));

        // 2. Check response body content
        String tsvBody = new String(response.getBody(), StandardCharsets.UTF_8);
        assertNotNull(tsvBody);

        // Check that the summary section contains the correct total revenue
        assertTrue("TSV does not contain correct total revenue",
                tsvBody.contains("Total Revenue\t" + expectedRevenue));

        // Check that the "Product Performance" section contains the products that were sold
        // (Names are normalized to lowercase)
        assertTrue("TSV does not contain Product A",
                tsvBody.contains(product1.getName().toLowerCase()));
        assertTrue("TSV does not contain Product B",
                tsvBody.contains(product2.getName().toLowerCase()));

        // Product C was not sold, so it should not be in the sales report
        assertFalse("TSV should not contain Product C",
                tsvBody.contains(product3.getName().toLowerCase()));
    }

    // --- getInventoryReport() Tests ---

    @Test
    public void getInventoryReportShouldReturnValidTsv() throws ApiException {
        // GIVEN
        // Data is setup in @Before

        // WHEN
        ResponseEntity<byte[]> response = reportDto.getInventoryReport();

        // THEN
        // 1. Check response headers
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        String header = response.getHeaders().get("Content-Disposition").get(0);
        assertTrue(header.contains("inventory-report.tsv"));

        // 2. Check response body content
        String tsvBody = new String(response.getBody(), StandardCharsets.UTF_8);
        assertNotNull(tsvBody);

        // Check summary data
        assertTrue("TSV does not contain correct total SKU count",
                tsvBody.contains("Total Product SKUs\t3"));
        assertTrue("TSV does not contain correct total inventory quantity",
                tsvBody.contains("Total Inventory Quantity\t" + expectedTotalStock));

        // Check item data (stock levels calculated in setUp)
        // (Names are normalized to lowercase)
        assertTrue("TSV does not contain correct data for Product A",
                tsvBody.contains(product1.getName().toLowerCase() + "\t70"));
        assertTrue("TSV does not contain correct data for Product B",
                tsvBody.contains(product2.getName().toLowerCase() + "\t94"));
        assertTrue("TSV does not contain correct data for Product C",
                tsvBody.contains(product3.getName().toLowerCase() + "\t100"));
    }
}