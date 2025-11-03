package com.increff.pos.integration.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.config.SpringConfig;
import com.increff.pos.dto.ProductDto;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.commons.exception.FormValidationException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.factory.ClientFactory;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.ProductForm;
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

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration Tests for the ProductDto class.
 * (Corrected for @Size(min=5) on barcode and lowercase normalization)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@WebAppConfiguration
@TestPropertySource("classpath:test.properties")
@Transactional
public class ProductDtoTest {

    @Autowired
    private ProductDto productDto; // Class under test

    // --- Setup Dependencies ---
    @Autowired
    private ClientApi clientApi;
    @Autowired
    private InventoryApi inventoryApi;
    @Autowired
    private ProductApi productApi;
    @Autowired
    private ProductFlow productFlow; // For delete

    // --- Prerequisite Data ---
    private Client client1;
    private Client client2;

    /**
     * Helper method to create a test Client ENTITY using the ClientApi.
     */
    private Client createTestClient(String name) throws ApiException {
        Client client = ClientFactory.mockNewObject(name);
        // We assume client names are also normalized on creation/retrieval
        return clientApi.insert(client);
    }

    /**
     * Helper to create a valid ProductForm for tests.
     * (Uses 5+ char barcodes)
     */
    private ProductForm createValidProductForm(Integer clientId, String barcode, String name, Double mrp, String category) {
        ProductForm form = new ProductForm();
        form.setClientId(clientId);
        form.setBarcode(barcode);
        form.setName(name);
        form.setMrp(mrp);
        form.setCategory(category);
        return form;
    }

    /**
     * Sets up prerequisite clients.
     */
    @Before
    public void setUp() throws ApiException {
        this.client1 = createTestClient("client a");
        this.client2 = createTestClient("client b");

    }

    // --- add() Tests ---

    @Test
    public void add_validProduct_shouldSaveAndCreateInventory() throws ApiException {
        // GIVEN
        ProductForm form = createValidProductForm(client1.getId(), "barcode-001", "Product 1", 100.0, "Category A");

        // WHEN
        ProductData data = productDto.add(form);

        // THEN
        // 1. Check returned DTO data
        assertNotNull(data.getId());
        assertEquals("product 1", data.getName()); // Check lowercase
        assertEquals("barcode-001", data.getBarcode()); // Check normalized barcode
        assertEquals(Double.valueOf(100.0), data.getMrp());
        assertEquals("client a", data.getClientName()); // Check lowercase
        assertEquals(Integer.valueOf(0), data.getQuantity());

        // 2. Check database state (Inventory)
        Inventory inv = inventoryApi.getCheckByProductId(data.getId());
        assertNotNull(inv);
        assertEquals(Integer.valueOf(0), inv.getQuantity());
    }

    @Test
    public void add_invalidData_shouldThrowValidationException() {
        // GIVEN
        ProductForm form = new ProductForm(); // Empty form
        form.setClientId(client1.getId());
        form.setMrp(-10.0); // Fails @Positive
        form.setBarcode("bc1"); // Fails @Size(min=5)
        form.setName(" "); // Fails @NotBlank

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class, () -> productDto.add(form));

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException fve = (FormValidationException) ex;
        Map<String, String> errors = fve.getErrors();

        // 4 errors: name(NotBlank), barcode(Size), mrp(Positive), category(NotBlank)
        assertEquals(4, errors.size());
        assertTrue(errors.containsKey("name"));
        assertTrue(errors.containsKey("barcode"));
        assertTrue(errors.containsKey("mrp"));
        assertTrue(errors.containsKey("category"));

        // Check correct error messages from annotations
        assertEquals("Product name cannot be blank", errors.get("name"));
        assertEquals("Barcode must be between 5 and 20 characters", errors.get("barcode"));
        assertEquals("MRP must be a positive value", errors.get("mrp"));
        assertEquals("Category cannot be blank", errors.get("category"));
    }

    @Test
    public void add_duplicateBarcode_shouldThrowApiException() throws ApiException {
        // GIVEN
        productDto.add(createValidProductForm(client1.getId(), "dup-barcode", "Product 1", 100.0, "Category A"));
        ProductForm duplicateForm = createValidProductForm(client2.getId(), "dup-barcode", "Product 2", 50.0, "Category B");

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class, () -> productDto.add(duplicateForm));
        assertEquals("Product already exists", ex.getMessage());
    }

    // --- getById() Tests ---

    @Test
    public void getById_validId_shouldReturnProduct() throws ApiException {
        // GIVEN
        ProductData addedProduct = productDto.add(createValidProductForm(client1.getId(), "barcode-001", "Product 1", 100.0, "Category A"));

        // WHEN
        ProductData fetchedData = productDto.getById(addedProduct.getId());

        // THEN
        assertNotNull(fetchedData);
        assertEquals(addedProduct.getId(), fetchedData.getId());
        assertEquals("product 1", fetchedData.getName());
        assertEquals("client a", fetchedData.getClientName());
    }

    @Test
    public void getById_nonExisting_shouldThrowApiException() {
        // GIVEN
        Integer nonExistentId = 99999;

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class, () -> productDto.getById(nonExistentId));
        assertEquals("Product " + nonExistentId + " doesn't exist", ex.getMessage());
    }

    // --- getByBarcode() Tests ---

    @Test
    public void getByBarcode_validBarcode_shouldReturnProduct() throws ApiException {
        // GIVEN
        ProductData addedProduct = productDto.add(createValidProductForm(client1.getId(), "barcode-123", "Product 1", 100.0, "Category A"));

        // WHEN
        ProductData fetchedData = productDto.getByBarcode("barcode-123");

        // THEN
        assertNotNull(fetchedData);
        assertEquals(addedProduct.getId(), fetchedData.getId());
        assertEquals("product 1", fetchedData.getName());
    }

    @Test
    public void getByBarcode_nonExisting_shouldThrowApiException() {
        // GIVEN
        String nonExistentBarcode = "non-existent-barcode";

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class, () -> productDto.getByBarcode(nonExistentBarcode));
        assertEquals("Product with barcode " + nonExistentBarcode + " doesn't exist", ex.getMessage());
    }

    // --- updateById() Tests ---

    @Test
    public void updateById_validData_shouldUpdate() throws ApiException {
        // GIVEN
        ProductData addedProduct = productDto.add(createValidProductForm(client1.getId(), "barcode-001", "Old Name", 100.0, "Old Category"));
        ProductForm updateForm = createValidProductForm(client1.getId(), "barcode-new", "New Name", 200.0, "New Category");

        // WHEN
        ProductData updatedData = productDto.updateById(addedProduct.getId(), updateForm);

        // THEN
        // 1. Check returned DTO
        assertEquals(addedProduct.getId(), updatedData.getId());
        assertEquals("new name", updatedData.getName());
        assertEquals("barcode-new", updatedData.getBarcode());
        assertEquals(Double.valueOf(200.0), updatedData.getMrp());

        // 2. Check persistence by fetching again
        ProductData fetchedData = productDto.getById(addedProduct.getId());
        assertEquals("new name", fetchedData.getName());
        assertEquals("barcode-new", fetchedData.getBarcode());
    }

    @Test
    public void updateById_changeClientId_shouldThrowApiException() throws ApiException {
        // GIVEN
        ProductData addedProduct = productDto.add(createValidProductForm(client1.getId(), "barcode-001", "Product 1", 100.0, "Category A"));
        ProductForm updateForm = createValidProductForm(client2.getId(), "barcode-001", "Product 1", 100.0, "Category A");

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class, () -> productDto.updateById(addedProduct.getId(), updateForm));
        assertEquals("Client id of a product can't be changed", ex.getMessage());
    }

    @Test
    public void updateById_duplicateBarcode_shouldThrowApiException() throws ApiException {
        // GIVEN
        productDto.add(createValidProductForm(client1.getId(), "barcode-001", "Product 1", 100.0, "Category A"));
        ProductData product2 = productDto.add(createValidProductForm(client1.getId(), "barcode-002", "Product 2", 100.0, "Category A"));

        ProductForm updateForm = createValidProductForm(client1.getId(), "barcode-001", "Product 2 Updated", 100.0, "Category A");

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class, () -> productDto.updateById(product2.getId(), updateForm));
        assertEquals("Another product with the barcode 'barcode-001' already exists.", ex.getMessage());
    }

    // --- deleteById() Tests ---

    @Test
    public void deleteById_valid_shouldDeleteProductAndInventory() throws ApiException {
        // GIVEN
        ProductData addedProduct = productDto.add(createValidProductForm(client1.getId(), "barcode-to-delete", "To Delete", 10.0, "Category A"));
        Integer productId = addedProduct.getId();

        assertNotNull(productApi.getCheckById(productId));
        assertNotNull(inventoryApi.getCheckByProductId(productId));

        // WHEN
        productDto.deleteById(productId);

        // THEN
        ApiException productEx = assertThrows(ApiException.class, () -> productApi.getCheckById(productId));
        assertEquals("Product " + productId + " doesn't exist", productEx.getMessage());

        ApiException invEx = assertThrows(ApiException.class, () -> inventoryApi.getCheckByProductId(productId));
        assertEquals("Inventory doesn't exist", invEx.getMessage());
    }

    // --- getFilteredProducts() Tests ---

    @Test
    public void getFilteredProducts_noFilter_shouldReturnAll() throws ApiException {
        // GIVEN
        productDto.add(createValidProductForm(client1.getId(), "barcode-001", "iPhone 10", 900.0, "Electronics"));
        productDto.add(createValidProductForm(client2.getId(), "barcode-002", "Galaxy S10", 800.0, "Electronics"));
        productDto.add(createValidProductForm(client1.getId(), "barcode-003", "iMac", 1500.0, "Computers"));

        // WHEN
        PaginationData<ProductData> result = productDto.getFilteredProducts(null, null, null, null, null, 5, 0);

        // THEN
        assertEquals(Long.valueOf(3), result.getTotalElements());
        assertEquals(3, result.getContent().size());
        assertEquals(Integer.valueOf(1), result.getTotalPages());
    }

    @Test
    public void getFilteredProducts_bySearchTerm_shouldReturnFiltered() throws ApiException {
        // GIVEN
        productDto.add(createValidProductForm(client1.getId(), "barcode-001", "iPhone 10", 900.0, "Electronics"));
        productDto.add(createValidProductForm(client2.getId(), "barcode-002", "Galaxy S10", 800.0, "Electronics"));
        productDto.add(createValidProductForm(client1.getId(), "barcode-003", "iMac", 1500.0, "Computers"));

        // WHEN
        // Search term "i" should be normalized and match "iPhone" and "iMac"
        PaginationData<ProductData> result = productDto.getFilteredProducts("i", null, null, null, null, 5, 0);

        // THEN
        assertEquals(Long.valueOf(3), result.getTotalElements());
        assertEquals(3, result.getContent().size());
        assertEquals("iphone 10", result.getContent().get(0).getName());
        assertEquals("galaxy s10", result.getContent().get(1).getName());
        assertEquals("imac", result.getContent().get(2).getName());
    }

    @Test
    public void getFilteredProducts_byClientName_shouldReturnFiltered() throws ApiException {
        // GIVEN
        productDto.add(createValidProductForm(client1.getId(), "barcode-001", "iPhone 10", 900.0, "Electronics"));
        productDto.add(createValidProductForm(client2.getId(), "barcode-002", "Galaxy S10", 800.0, "Electronics"));
        productDto.add(createValidProductForm(client1.getId(), "barcode-003", "iMac", 1500.0, "Computers"));

        // WHEN
        PaginationData<ProductData> result = productDto.getFilteredProducts(null, "Client A", null, null, null, 5, 0);

        // THEN
        assertEquals(Long.valueOf(2), result.getTotalElements()); // iPhone, iMac
        assertEquals(2, result.getContent().size());
        assertEquals("client a", result.getContent().get(0).getClientName());
    }

    @Test
    public void getFilteredProducts_byMrpRange_shouldReturnFiltered() throws ApiException {
        // GIVEN
        productDto.add(createValidProductForm(client1.getId(), "barcode-001", "iPhone 10", 900.0, "Electronics"));
        productDto.add(createValidProductForm(client2.getId(), "barcode-002", "Galaxy S10", 800.0, "Electronics"));
        productDto.add(createValidProductForm(client1.getId(), "barcode-003", "iMac", 1500.0, "Computers"));

        // WHEN
        PaginationData<ProductData> result = productDto.getFilteredProducts(null, null, null, 1000.0, 2000.0, 5, 0);

        // THEN
        assertEquals(Long.valueOf(1), result.getTotalElements()); // iMac
        assertEquals("imac", result.getContent().get(0).getName());
    }

    @Test
    public void getFilteredProducts_pagination_shouldWork() throws ApiException {
        // GIVEN
        ProductData p1 = productDto.add(createValidProductForm(client1.getId(), "barcode-001", "p1", 10.0, "cat"));
        ProductData p2 = productDto.add(createValidProductForm(client1.getId(), "barcode-002", "p2", 10.0, "cat"));
        ProductData p3 = productDto.add(createValidProductForm(client1.getId(), "barcode-003", "p3", 10.0, "cat"));

        // WHEN: Get Page 0, Size 2
        PaginationData<ProductData> page1 = productDto.getFilteredProducts(null, null, null, null, null, 2, 0);

        // THEN
        assertEquals(Long.valueOf(3), page1.getTotalElements());
        assertEquals(Integer.valueOf(2), page1.getTotalPages());
        assertEquals(2, page1.getContent().size());
        assertEquals(p1.getId(), page1.getContent().get(0).getId());
        assertEquals(p2.getId(), page1.getContent().get(1).getId());

        // WHEN: Get Page 1, Size 2
        PaginationData<ProductData> page2 = productDto.getFilteredProducts(null, null, null, null, null, 2, 1);

        // THEN
        assertEquals(Long.valueOf(3), page2.getTotalElements());
        assertEquals(Integer.valueOf(2), page2.getTotalPages());
        assertEquals(1, page2.getContent().size());
        assertEquals(p3.getId(), page2.getContent().get(0).getId());
    }

    // --- uploadByFile() Tests ---

    @Test
    public void uploadByFile_validTsv_shouldWork() throws ApiException, IOException {
        // GIVEN
        // Use 5+ char barcodes
        String tsvContent = "barcode\tname\tmrp\tclientName\tcategory\n"
                + "tsv-bc-01\tTSV Product 1\t10.50\tClient A\tTSV Cat\n"
                + "tsv-bc-02\tTSV Product 2\t20.00\tClient B\tTSV Cat";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.tsv",
                "text/tab-separated-values",
                tsvContent.getBytes()
        );

        // WHEN
        ResponseEntity<byte[]> response = productDto.uploadByFile(file);

        // THEN
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getHeaders().get("Content-Disposition").get(0).contains("product-upload-report.tsv"));

        // Barcodes are normalized to lowercase by the DTO
        ProductData p1 = productDto.getByBarcode("tsv-bc-01");
        ProductData p2 = productDto.getByBarcode("tsv-bc-02");

        assertNotNull(p1);
        assertNotNull(p2);
        assertEquals("tsv product 1", p1.getName()); // Check lowercase
        assertEquals("client b", p2.getClientName()); // Check lowercase

        assertEquals(Integer.valueOf(0), p1.getQuantity());
        assertEquals(Integer.valueOf(0), p2.getQuantity());
    }
}