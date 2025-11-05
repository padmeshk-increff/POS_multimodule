package com.increff.pos.integration.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.config.SpringConfig;
import com.increff.pos.dto.OrderItemDto;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.factory.OrderFactory;
import com.increff.pos.factory.OrderItemFactory;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.commons.exception.FormValidationException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.factory.ClientFactory;
import com.increff.pos.factory.InventoryFactory;
import com.increff.pos.factory.ProductFactory;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderItemUpdateForm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration Tests for the OrderItemDto class.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@WebAppConfiguration
@TestPropertySource("classpath:test.properties")
@Transactional
public class OrderItemDtoTest {

    @Autowired
    private OrderItemDto orderItemDto; // Class under test

    // --- Setup Dependencies ---
    @Autowired
    private OrderFlow orderFlow;

    @Autowired
    private ClientApi clientApi;
    @Autowired
    private ProductFlow productFlow;
    @Autowired
    private InventoryApi inventoryApi;
    @Autowired
    private OrderApi orderApi; // For invoicing/modifying order state
    @Autowired
    private ProductApi productApi; // For checking product details

    // --- Prerequisite Data ---
    private Client testClient;
    private Product product1, product2, product3;
    private Order order1;
    private Integer order1Id;
    private Integer item1Id; // (product1 in order1)
    private Integer item2Id; // (product2 in order1)

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
     * Sets up prerequisite data:
     * 1 Client
     * 3 Products (2 in an order, 1 spare)
     * 1 Order (Status: CREATED) containing 2 OrderItems
     */
    @Before
    public void setUp() throws ApiException {
        // 1. Setup prerequisite products (same as before)
        this.testClient = createTestClient("test-client");
        this.product1 = createTestProduct(testClient.getId(), "barcode1", "Product A", 100.0, 50);
        this.product2 = createTestProduct(testClient.getId(), "barcode2", "Product B", 50.0, 50);
        this.product3 = createTestProduct(testClient.getId(), "barcode3", "Product C", 20.0, 50);

        // 2. Create transient entities using factories
        Order order = OrderFactory.mockNewObject();
        order.setCustomerPhone("1234567890");

        // Create 10 of product1 (stock becomes 40)
        OrderItem oi1 = OrderItemFactory.mockNewObject(null, product1.getId(), 10, 90.0);

        // Create 5 of product2 (stock becomes 45)
        OrderItem oi2 = OrderItemFactory.mockNewObject(null, product2.getId(), 5, 50.0);

        List<OrderItem> items = Arrays.asList(oi1, oi2);

        // 3. Call OrderFlow.insert() directly (NO DTO layer involved)
        OrderResult result = orderFlow.insert(order, items);

        // 4. Store the persisted entities and their IDs for the tests
        this.order1 = result.getOrder();
        this.order1Id = this.order1.getId();

        // Get the persisted OrderItem entities from the result
        OrderItem item1Entity = result.getOrderItems().stream()
                .filter(i -> i.getProductId().equals(product1.getId()))
                .findFirst().get();
        OrderItem item2Entity = result.getOrderItems().stream()
                .filter(i -> i.getProductId().equals(product2.getId()))
                .findFirst().get();

        this.item1Id = item1Entity.getId();
        this.item2Id = item2Entity.getId();
    }

    // --- add() Tests ---

    @Test
    public void addValidItemShouldWork() throws ApiException {
        // GIVEN
        // Add product3 (50 in stock) to the existing order
        OrderItemForm form = new OrderItemForm();
        form.setProductId(product3.getId());
        form.setQuantity(20);
        form.setSellingPrice(15.0);

        // WHEN
        OrderItemData newItem = orderItemDto.add(order1Id, form);

        // THEN
        assertNotNull(newItem);
        assertNotNull(newItem.getId());
        assertEquals(Integer.valueOf(20), newItem.getQuantity());
        // --- THIS IS THE FIX ---
        // Assert the product name, not the ID, as per the OrderItemData DTO
        assertEquals(product3.getName(), newItem.getProductName());

        // Check inventory deduction
        Inventory updatedInventory = inventoryApi.getCheckByProductId(product3.getId());
        assertEquals(Integer.valueOf(30), updatedInventory.getQuantity()); // 50 - 20

        // Check if order total amount was updated (Original: 10*90 + 5*50 = 900 + 250 = 1150)
        Order updatedOrder = orderApi.getCheckById(order1Id);
        Double expectedAmount = 1150.0 + (20 * 15.0); // 1150 + 300 = 1450
        assertEquals(expectedAmount, updatedOrder.getTotalAmount());
    }

    @Test
    public void addInvalidDataShouldThrowValidationException() {
        // GIVEN
        OrderItemForm form = new OrderItemForm();
        form.setProductId(product3.getId());
        form.setQuantity(-5); // Fails @Positive
        form.setSellingPrice(15.0);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.add(order1Id, form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException validationEx = (FormValidationException) ex;
        Map<String, String> errors = validationEx.getErrors();

        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("quantity"));
        assertEquals("Quantity must be a positive number", errors.get("quantity"));
    }

    @Test
    public void addToInvoicedOrderShouldThrowApiException() throws ApiException {
        // GIVEN
        orderApi.updateInvoiceOrder(order1Id); // Invoice the order
        OrderItemForm form = new OrderItemForm();
        form.setProductId(product3.getId());
        form.setQuantity(20);
        form.setSellingPrice(15.0);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.add(order1Id, form)
        );
        assertEquals("Cannot modify an order that is already INVOICED", ex.getMessage());
    }

    @Test
    public void addInsufficientInventoryShouldThrowApiException() {
        // GIVEN
        // Add product3 (50 in stock) but ask for 60
        OrderItemForm form = new OrderItemForm();
        form.setProductId(product3.getId());
        form.setQuantity(60);
        form.setSellingPrice(15.0);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.add(order1Id, form)
        );
        // --- THIS IS THE FIX ---
        // Use the exact error message from OrderFlow
        String expectedMsg = "Not enough stock is available for product with id " + product3.getId();
        assertEquals(expectedMsg, ex.getMessage());
    }

    @Test
    public void addSellingPriceGreaterThanMrpShouldThrowApiException() {
        // GIVEN
        // Add product3 (MRP 20.0) but sell for 25.0
        OrderItemForm form = new OrderItemForm();
        form.setProductId(product3.getId());
        form.setQuantity(10);
        form.setSellingPrice(25.0);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.add(order1Id, form)
        );
        String expectedMsg = "Selling price for product '" + product3.getName() + "' cannot be greater than its MRP: " + product3.getMrp();
        assertEquals(expectedMsg, ex.getMessage());
    }

    // --- updateById() Tests ---

    @Test
    public void updateByIdValidDataShouldWork() throws ApiException {
        // GIVEN
        // item1Id is 10 * product1 (stock 40 after setup)
        // Update to 20 * product1
        OrderItemUpdateForm form = new OrderItemUpdateForm();
        form.setQuantity(20);
        form.setSellingPrice(80.0); // Old price was 90.0

        // WHEN
        OrderItemData updatedItem = orderItemDto.updateById(order1Id, item1Id, form);

        // THEN
        assertNotNull(updatedItem);
        assertEquals(item1Id, updatedItem.getId());
        assertEquals(Integer.valueOf(20), updatedItem.getQuantity());
        assertEquals(Double.valueOf(80.0), updatedItem.getSellingPrice());

        // Check inventory: 50 (start) - 10 (setup) = 40. 40 + 10 (revert old) - 20 (apply new) = 30
        Inventory updatedInventory = inventoryApi.getCheckByProductId(product1.getId());
        assertEquals(Integer.valueOf(30), updatedInventory.getQuantity());

        Order updatedOrder = orderApi.getCheckById(order1Id);
        // New total = 1150.0 (original) - 900.0 (old) + 1600.0 (new) = 1850.0
        Double expectedTotal = 1150.0 - (10 * 90.0) + (20 * 80.0);
        assertEquals(expectedTotal, updatedOrder.getTotalAmount());
    }

    @Test
    public void updateByIdInvalidDataShouldThrowValidationException() {
        // GIVEN
        OrderItemUpdateForm form = new OrderItemUpdateForm();
        form.setQuantity(-5); // Fails @Positive
        form.setSellingPrice(80.0);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.updateById(order1Id, item1Id, form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException validationEx = (FormValidationException) ex;
        Map<String, String> errors = validationEx.getErrors();

        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("quantity"));
        // --- THIS IS THE FIX ---
        // Add assertion for the message
        assertEquals("Quantity must be a positive number", errors.get("quantity"));
    }

    @Test
    public void updateByIdOnInvoicedOrderShouldThrowApiException() throws ApiException {
        // GIVEN
        orderApi.updateInvoiceOrder(order1Id); // Invoice the order
        OrderItemUpdateForm form = new OrderItemUpdateForm();
        form.setQuantity(20);
        form.setSellingPrice(80.0);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.updateById(order1Id, item1Id, form)
        );
        assertEquals("Cannot modify an order that is already INVOICED", ex.getMessage());
    }

    // --- deleteById() Tests ---

    @Test
    public void deleteByIdValidShouldWork() throws ApiException {
        // GIVEN
        // order1 has 2 items. Stock of p1 is 40 (after setup).
        // Order total is 1150.0
        Double item1Value = 10 * 90.0; // 900.0

        // WHEN
        orderItemDto.deleteById(order1Id, item1Id);

        // THEN
        // 1. Check item list size
        List<OrderItemData> remainingItems = orderItemDto.getByOrderId(order1Id);
        assertEquals(1, remainingItems.size());
        assertEquals(item2Id, remainingItems.get(0).getId());

        // 2. Check inventory (stock was 40, should be 50 again)
        Inventory updatedInventory = inventoryApi.getCheckByProductId(product1.getId());
        assertEquals(Integer.valueOf(50), updatedInventory.getQuantity());

        // 3. Check order total
        Order updatedOrder = orderApi.getCheckById(order1Id);
        assertEquals(Double.valueOf(1150.0 - item1Value), updatedOrder.getTotalAmount());
    }

    @Test
    public void deleteByIdOnInvoicedOrderShouldThrowApiException() throws ApiException {
        // GIVEN
        orderApi.updateInvoiceOrder(order1Id); // Invoice the order

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.deleteById(order1Id, item1Id)
        );
        assertEquals("Cannot modify an order that is already INVOICED", ex.getMessage());
    }

    // --- getAll() Tests ---

    @Test
    public void getAllWithDataShouldReturnList() throws ApiException {
        // GIVEN (Setup creates 2 order items)

        // WHEN
        List<OrderItemData> allItems = orderItemDto.getAll();

        // THEN
        assertEquals(2, allItems.size());
    }

    // --- getByOrderId() Tests ---

    @Test
    public void getByOrderIdValidIdShouldReturnList() throws ApiException {
        // GIVEN
        // order1Id from setup

        // WHEN
        List<OrderItemData> items = orderItemDto.getByOrderId(order1Id);

        // THEN
        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(item -> item.getId().equals(item1Id)));
        assertTrue(items.stream().anyMatch(item -> item.getId().equals(item2Id)));
    }

    @Test
    public void getByOrderIdNonExistingIdShouldReturnEmptyList() throws ApiException {
        // GIVEN
        Integer nonExistentOrderId = 9999;

        // WHEN
        List<OrderItemData> items = orderItemDto.getByOrderId(nonExistentOrderId);

        // THEN
        assertNotNull(items);
        assertEquals(0, items.size());
    }

    @Test
    public void addNullFieldsShouldThrowValidationException() {
        // GIVEN
        // Create a form with all fields null
        OrderItemForm form = new OrderItemForm();

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.add(order1Id, form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException validationEx = (FormValidationException) ex;
        Map<String, String> errors = validationEx.getErrors();

        // All 3 fields should fail the @NotNull check
        assertEquals(3, errors.size());
        assertTrue(errors.containsKey("productId"));
        assertTrue(errors.containsKey("quantity"));
        assertTrue(errors.containsKey("sellingPrice"));
        assertEquals("ProductId cannot be null", errors.get("productId"));
    }

    @Test
    public void updateNegativePriceShouldThrowValidationException() {
        // GIVEN
        // OrderItemUpdateForm has @Positive validation on price
        OrderItemUpdateForm form = new OrderItemUpdateForm();
        form.setQuantity(10); // Valid quantity
        form.setSellingPrice(-5.0); // Invalid price

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderItemDto.updateById(order1Id, item1Id, form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException validationEx = (FormValidationException) ex;
        Map<String, String> errors = validationEx.getErrors();

        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("sellingPrice"));
        assertEquals("Selling price must be a positive number", errors.get("sellingPrice"));
    }

}