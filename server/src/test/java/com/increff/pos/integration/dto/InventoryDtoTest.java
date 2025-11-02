package com.increff.pos.integration.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.config.SpringConfig;
import com.increff.pos.dto.InventoryDto;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.InventoryData;
import com.increff.pos.model.form.InventoryForm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import org.hibernate.PropertyValueException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration Tests for the InventoryDto class.
 * This test file validates the functional contract of the DTO layer
 * by interacting with a real (test) database.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@TestPropertySource(locations = "classpath:test.properties")
@WebAppConfiguration
@Transactional // Automatically rolls back database changes after each test
public class InventoryDtoTest {

    @Autowired
    private InventoryDto inventoryDto;

    // We need these to set up the prerequisite data
    @Autowired
    private ProductFlow productFlow;
    @Autowired
    private ClientApi clientApi;
    @Autowired
    private InventoryApi inventoryApi; // To get the generated ID

    private Product testProduct;
    private Inventory testInventory;

    @Before
    public void setUp() throws ApiException {
        // --- GIVEN ---
        // Inventory cannot exist without a Product and Client.
        // We must create these prerequisites first.
        Client c = new Client();
        c.setClientName("test-client");
        clientApi.insert(c);

        Product p = new Product();
        p.setBarcode("barcode123");
        p.setClientId(c.getId());
        p.setName("Test Product");
        p.setMrp(100.0);
        p.setCategory("test-cat");

        // Use the flow to insert the product AND its initial inventory
        testProduct = productFlow.insert(p);

        // Get the generated inventory record
        testInventory = inventoryApi.getCheckByProductId(testProduct.getId());
        assertNotNull(testInventory);
        assertEquals(0, (int) testInventory.getQuantity());
    }

    // --- getById() Tests ---

    @Test
    public void getById_happyPath_shouldReturnInventory() throws ApiException {
        // WHEN
        InventoryData data = inventoryDto.getById(testInventory.getId());

        // THEN
        assertNotNull(data);
        assertEquals(testInventory.getId(), data.getId());
        assertEquals(testProduct.getId(), data.getProductId());
        assertEquals(0, (int) data.getQuantity());
    }

    @Test
    public void getById_notFound_shouldThrowException() {
        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> inventoryDto.getById(9999)
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // --- getAll() Tests ---

    @Test
    public void getAll_shouldReturnList() throws ApiException {
        // GIVEN: One item was added in @Before

        // WHEN
        List<InventoryData> list = inventoryDto.getAll();

        // THEN
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(testInventory.getId(), list.get(0).getId());
    }

    // --- updateById() Tests ---

    @Test
    public void updateById_happyPath_shouldUpdateQuantity() throws ApiException {
        // GIVEN
        InventoryForm form = new InventoryForm();
        form.setQuantity(50);

        // WHEN
        InventoryData updatedData = inventoryDto.updateById(testInventory.getId(), form);

        // THEN
        assertEquals(50, (int) updatedData.getQuantity());

        // Verify in DB
        InventoryData fromDb = inventoryDto.getById(testInventory.getId());
        assertEquals(50, (int) fromDb.getQuantity());
    }

    @Test
    public void updateById_notFound_shouldThrowException() {
        // GIVEN
        InventoryForm form = new InventoryForm();
        form.setQuantity(50);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> inventoryDto.updateById(9999, form)
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    @Test
    public void updateById_nullQuantity_shouldThrowException() {
        // GIVEN
        InventoryForm form = new InventoryForm();
        form.setQuantity(null); // Set quantity to null

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> inventoryDto.updateById(testInventory.getId(), form)
        );
        assertEquals("Quantity cannot be null", ex.getMessage());
    }

    // --- updateByProductId() Tests ---

    @Test
    public void updateByProductId_happyPath_shouldUpdateQuantity() throws ApiException {
        // GIVEN
        InventoryForm form = new InventoryForm();
        form.setQuantity(75);

        // WHEN
        InventoryData updatedData = inventoryDto.updateByProductId(testProduct.getId(), form);

        // THEN
        assertEquals(75, (int) updatedData.getQuantity());

        // Verify in DB
        InventoryData fromDb = inventoryDto.getById(testInventory.getId());
        assertEquals(75, (int) fromDb.getQuantity());
    }

    @Test
    public void updateByProductId_notFound_shouldThrowException() {
        // GIVEN
        InventoryForm form = new InventoryForm();
        form.setQuantity(50);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> inventoryDto.updateByProductId(9999, form) // Non-existent product ID
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // --- uploadByFile() Tests ---

    @Test
    public void uploadByFile_happyPath_shouldUpdateInventory() throws ApiException {
        // GIVEN
        // Our testInventory (for product "barcode123") has quantity 0
        String tsv = "barcode\tquantity\nbarcode123\t42\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "inventory.tsv",
                "text/tab-separated-values",
                tsv.getBytes()
        );

        // WHEN
        ResponseEntity<byte[]> response = inventoryDto.uploadByFile(file);

        // THEN
        // 1. Check the report
        assertNotNull(response.getBody());
        String report = new String(response.getBody());
        assertTrue(report.contains("barcode\tquantity\tstatus/error"));
        assertTrue(report.contains("barcode123\t42\tSUCCESS"));

        // 2. Verify the change in the database
        InventoryData fromDb = inventoryDto.getById(testInventory.getId());
        assertEquals(42, (int) fromDb.getQuantity());
    }

    @Test
    public void uploadByFile_badHeader_shouldThrowApiException() {
        // GIVEN
        String tsv = "bad_header\tqty\nbarcode123\t50\n";
        MockMultipartFile file = new MockMultipartFile("file", "inventory.tsv", "text/plain", tsv.getBytes());

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> inventoryDto.uploadByFile(file)
        );
        assertEquals("Invalid file headers. Expected columns: [barcode, quantity], but found: [bad_header, qty]", ex.getMessage());
    }

    @Test
    public void uploadByFile_rowError_shouldNotUpdateAndReturnReport() throws ApiException {
        // GIVEN
        // Error 1: "fifty" is not a valid quantity
        // Error 2: "non-existent-bc" is not in the Product table
        String tsv = "barcode\tquantity\nbarcode123\tfifty\nnon-existent-bc\t10\n";
        MockMultipartFile file = new MockMultipartFile("file", "inventory.tsv", "text/plain", tsv.getBytes());

        // WHEN
        ResponseEntity<byte[]> response = inventoryDto.uploadByFile(file);

        // THEN
        // 1. Check the report
        assertNotNull(response.getBody());
        String report = new String(response.getBody());
        assertTrue(report.contains("barcode\tquantity\tstatus/error"));
        assertTrue(report.contains("non-existent-bc\t10\tProduct with barcode 'non-existent-bc' does not exist."));
        assertTrue(report.contains("--- The following rows could not be parsed ---"));
        assertTrue(report.contains("Error in row #2: Invalid number format for quantity: 'fifty'"));

        // 2. Verify NO change in the database
        InventoryData fromDb = inventoryDto.getById(testInventory.getId());
        assertEquals(0, (int) fromDb.getQuantity()); // Should still be 0
    }
}