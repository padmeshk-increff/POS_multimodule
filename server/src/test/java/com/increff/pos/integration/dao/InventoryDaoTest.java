package com.increff.pos.integration.dao;

import com.increff.pos.config.TestDbConfig;
import com.increff.pos.dao.ClientDao;
import com.increff.pos.dao.InventoryDao;
import com.increff.pos.dao.ProductDao;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.result.InventoryReportResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import com.increff.pos.factory.ClientFactory;
import com.increff.pos.factory.ProductFactory;
import com.increff.pos.factory.InventoryFactory;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestDbConfig.class)
@TestPropertySource("classpath:test.properties")
@Transactional // Automatically rolls back database changes after each test
public class InventoryDaoTest {

    @Autowired
    private InventoryDao inventoryDao;

    // DAOs required to set up prerequisite data
    @Autowired
    private ProductDao productDao;
    @Autowired
    private ClientDao clientDao;

    private Client testClient;
    private Product testProduct1;
    private Product testProduct2;

    @Before
    public void setUp() {
        // We must set up the foreign key relationships
        // for our inventory tests to work
        testClient = ClientFactory.mockNewObject("test-client");
        clientDao.insert(testClient);

        testProduct1 = createTestProduct("barcode1", testClient);
        testProduct2 = createTestProduct("barcode2", testClient);
    }

    // --- Helper method to create a Product (Prerequisite) ---
    private Product createTestProduct(String barcode, Client client) {
        Product p = ProductFactory.mockNewObject(barcode, client.getId());
        productDao.insert(p);
        assertNotNull(p.getId()); // Ensure product was saved
        return p;
    }

    // --- Helper method to create an Inventory item ---
    private Inventory createTestInventory(Product product, int quantity) {
        Inventory i = InventoryFactory.mockNewObject(product.getId());
        i.setQuantity(quantity); // Override the default quantity
        inventoryDao.insert(i);
        assertNotNull(i.getId()); // Ensure inventory was saved
        return i;
    }

    // --- Tests for AbstractDao methods ---

    @Test
    public void testInsertAndSelectById() {
        // Act
        Inventory i = createTestInventory(testProduct1, 50);

        // Assert
        Inventory fromDb = inventoryDao.selectById(i.getId());
        assertNotNull(fromDb);
        assertEquals(testProduct1.getId(), fromDb.getProductId());
        assertEquals(50, (int) fromDb.getQuantity());
    }

    @Test
    public void testUpdate() {
        // Arrange
        Inventory i = createTestInventory(testProduct1, 50);

        // Act
        i.setQuantity(99);
        inventoryDao.update(i);
        Inventory fromDb = inventoryDao.selectById(i.getId());

        // Assert
        assertEquals(99, (int) fromDb.getQuantity());
    }

    @Test
    public void testDeleteById() {
        // Arrange
        Inventory i = createTestInventory(testProduct1, 50);
        Integer id = i.getId();

        // Act
        inventoryDao.deleteById(id);
        Inventory fromDb = inventoryDao.selectById(id);

        // Assert
        assertNull(fromDb);
    }

    // --- Tests for InventoryDao specific methods ---

    @Test
    public void testSelectByProductId() {
        // Arrange
        createTestInventory(testProduct1, 50);

        // Act
        Inventory fromDb = inventoryDao.selectByProductId(testProduct1.getId());

        // Assert
        assertNotNull(fromDb);
        assertEquals(50, (int) fromDb.getQuantity());
    }

    @Test
    public void testSelectByProductIdNotFound() {
        // Act
        Inventory fromDb = inventoryDao.selectByProductId(9999);
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testSelectByProductIds() {
        // Arrange
        createTestInventory(testProduct1, 50);
        createTestInventory(testProduct2, 100);
        List<Integer> productIds = Arrays.asList(testProduct1.getId(), testProduct2.getId());

        // Act
        List<Inventory> fromDb = inventoryDao.selectByProductIds(productIds);

        // Assert
        assertEquals(2, fromDb.size());
    }

    @Test
    public void testSelectLowStockItems() {
        // Arrange
        int initialLowStockCount = inventoryDao.selectLowStockItems(10).size();
        Product p3 = createTestProduct("barcode3", testClient);
        createTestInventory(testProduct1, 5);  // Low stock
        createTestInventory(testProduct2, 20); // In stock
        createTestInventory(p3, 9);  // Low stock

        // Act
        List<Inventory> lowStock = inventoryDao.selectLowStockItems(10); // Threshold is 10

        // Assert
        assertEquals(initialLowStockCount + 2, lowStock.size());
        // Our 2 low stock items should be in the list (we can't guarantee exact position due to existing data)
        assertTrue(lowStock.stream().anyMatch(i -> i.getQuantity() == 5));
        assertTrue(lowStock.stream().anyMatch(i -> i.getQuantity() == 9));
    }

    @Test
    public void testBulkUpdate() {
        // Arrange
        // 1. Create initial state in DB
        Inventory i1_db = createTestInventory(testProduct1, 100);
        Inventory i2_db = createTestInventory(testProduct2, 200);

        // 2. Create in-memory "update" objects using factory
        Inventory i1_update = InventoryFactory.mockNewObject(testProduct1.getId());
        i1_update.setQuantity(5); // New quantity

        Inventory i2_update = InventoryFactory.mockNewObject(testProduct2.getId());
        i2_update.setQuantity(10); // New quantity

        List<Inventory> updates = Arrays.asList(i1_update, i2_update);

        // Act
        inventoryDao.bulkUpdate(updates);

        // Assert
        Inventory i1_fromDb = inventoryDao.selectById(i1_db.getId());
        Inventory i2_fromDb = inventoryDao.selectById(i2_db.getId());

        assertEquals(5, (int) i1_fromDb.getQuantity());
        assertEquals(10, (int) i2_fromDb.getQuantity());
    }

    @Test
    public void testBulkUpdateIgnoresNonExistentProductId() {
        // Arrange
        Inventory i1_db = createTestInventory(testProduct1, 100);

        // This update is for a product that has no inventory record
        Inventory i2_update = InventoryFactory.mockNewObject(testProduct2.getId());
        i2_update.setQuantity(10);

        // This update is for a product that doesn't even exist
        Inventory i3_update = InventoryFactory.mockNewObject(9999);
        i3_update.setQuantity(20);

        // Act
        // This should not throw an error, it should just do nothing for i2 and i3
        inventoryDao.bulkUpdate(Arrays.asList(i2_update, i3_update));

        // Assert
        // The original inventory item should be unchanged
        Inventory i1_fromDb = inventoryDao.selectById(i1_db.getId());
        assertEquals(100, (int) i1_fromDb.getQuantity());
    }

    @Test
    public void testFindInventoryReportData() {
        // Arrange
        int initialReportSize = inventoryDao.findInventoryReportData().size();
        Product p_apple = createTestProduct("bc-a", testClient);
        Product p_banana = createTestProduct("bc-b", testClient);
        createTestInventory(p_apple, 50);
        createTestInventory(p_banana, 100);

        // Act
        List<InventoryReportResult> report = inventoryDao.findInventoryReportData();

        // Assert
        assertEquals(initialReportSize + 2, report.size());

        // Find our specific products in the report
        InventoryReportResult appleReport = report.stream()
                .filter(r -> r.getProductId().equals(p_apple.getId()))
                .findFirst()
                .orElse(null);
        InventoryReportResult bananaReport = report.stream()
                .filter(r -> r.getProductId().equals(p_banana.getId()))
                .findFirst()
                .orElse(null);

        // Verify our products are in the report with correct data
        assertNotNull(appleReport);
        assertEquals(p_apple.getBarcode(), appleReport.getBarcode());
        assertEquals(p_apple.getCategory(), appleReport.getCategory());
        assertEquals(p_apple.getMrp(), appleReport.getMrp(), 0.001);
        assertEquals(50, (int) appleReport.getQuantity());

        assertNotNull(bananaReport);
        assertEquals(p_banana.getId(), bananaReport.getProductId());
        assertEquals(100, (int) bananaReport.getQuantity());
    }
}