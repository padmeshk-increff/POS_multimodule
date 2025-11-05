package com.increff.pos.integration.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.config.SpringConfig;
import com.increff.pos.dto.OrderDto;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.commons.exception.FormValidationException; // Import the exception
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.factory.ClientFactory;
import com.increff.pos.factory.InventoryFactory;
import com.increff.pos.factory.ProductFactory;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderForm;
import com.increff.pos.model.form.OrderUpdateForm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration Tests for the OrderDto class.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@WebAppConfiguration
@TestPropertySource("classpath:test.properties")
@Transactional
public class OrderDtoTest {

    @Autowired
    private OrderDto orderDto;

    // --- Layers used for Test Setup ---
    @Autowired
    private ClientApi clientApi;
    @Autowired
    private ProductFlow productFlow;
    @Autowired
    private ProductApi productApi;
    @Autowired
    private InventoryApi inventoryApi;

    // --- Class members to hold prerequisite ENTITIES ---
    private Client testClient;
    private Product product1;
    private Product product2;

    /**
     * Helper method to create a test Client ENTITY using the ClientApi.
     */
    private Client createTestClient(String name) throws ApiException {
        Client client = ClientFactory.mockNewObject(name);
        return clientApi.insert(client);
    }

    /**
     * Helper method to create a Product ENTITY and set its Inventory
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
     * Helper method to create a valid OrderForm for tests.
     */
    private OrderForm createValidOrderForm() {
        OrderItemForm item1 = new OrderItemForm();
        item1.setProductId(product1.getId());
        item1.setQuantity(5);
        item1.setSellingPrice(90.0);

        OrderItemForm item2 = new OrderItemForm();
        item2.setProductId(product2.getId());
        item2.setQuantity(10);
        item2.setSellingPrice(25.50);

        OrderForm orderForm = new OrderForm();
        orderForm.setCustomerPhone("9876543210"); // Valid phone number (no spaces)
        orderForm.setItems(Arrays.asList(item1, item2));
        return orderForm;
    }

    /**
     * Sets up prerequisite data (Client, Products, Inventory) before each test
     */
    @Before
    public void setUp() throws ApiException {
        this.testClient = createTestClient("test-client");
        this.product1 = createTestProduct(testClient.getId(), "barcode1", "Product A", 100.0, 50);
        this.product2 = createTestProduct(testClient.getId(), "barcode2", "Product B", 25.50, 75);
    }

    // --- add() Tests ---

    @Test
    public void addValidOrderShouldSaveAndDeductInventory() throws ApiException {
        // GIVEN
        OrderForm orderForm = createValidOrderForm();

        // WHEN
        OrderData orderData = orderDto.add(orderForm);

        // THEN
        assertNotNull(orderData);
        assertNotNull(orderData.getId());
        assertEquals("9876543210", orderData.getCustomerPhone());
        assertEquals(OrderStatus.CREATED, orderData.getOrderStatus());
        assertEquals(2, orderData.getOrderItemDataList().size());

        assertEquals(Integer.valueOf(45), inventoryApi.getCheckByProductId(product1.getId()).getQuantity());
        assertEquals(Integer.valueOf(65), inventoryApi.getCheckByProductId(product2.getId()).getQuantity());
    }

    @Test
    public void addEmptyItemsShouldThrowException() {
        // GIVEN
        OrderForm orderForm = new OrderForm();
        orderForm.setCustomerPhone("1234567890");
        orderForm.setItems(Collections.emptyList()); // Fails @NotEmpty

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderDto.add(orderForm)
        );

        // --- THIS IS THE FIX ---
        // Assert it's the specific FormValidationException
        assertTrue("Exception was not a FormValidationException",
                ex instanceof FormValidationException);

        // Cast it and get the errors map
        FormValidationException validationEx = (FormValidationException) ex;
        Map<String, String> errors = validationEx.getErrors();

        // Check the map for the specific field error
        String expectedField = "items";
        String expectedMessage = "Order must contain at least one item";

        assertEquals("The errors map should have one entry", 1, errors.size());
        assertTrue("Errors map does not contain key: " + expectedField,
                errors.containsKey(expectedField));
        assertEquals("Incorrect validation message for " + expectedField,
                expectedMessage, errors.get(expectedField));
    }

    @Test
    public void addInvalidPhoneShouldThrowException() {
        // GIVEN
        OrderForm orderForm = createValidOrderForm();
        orderForm.setCustomerPhone("invalid-phone-number"); // Fails @Pattern

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderDto.add(orderForm)
        );

        // Assert it's the specific FormValidationException
        assertTrue("Exception was not a FormValidationException",
                ex instanceof FormValidationException);

        // Cast it and get the errors map
        FormValidationException validationEx = (FormValidationException) ex;
        Map<String, String> errors = validationEx.getErrors();

        // Check the map for the specific field error
        String expectedField = "customerPhone";
        String expectedMessage = "Phone number must contain only digits";

        assertEquals("The errors map should have one entry", 1, errors.size());
        assertTrue("Errors map does not contain key: " + expectedField,
                errors.containsKey(expectedField));
        assertEquals("Incorrect validation message for " + expectedField,
                expectedMessage, errors.get(expectedField));
    }

    @Test
    public void addInsufficientInventoryShouldThrowException() {
        // GIVEN
        OrderItemForm item1 = new OrderItemForm();
        item1.setProductId(product1.getId());
        item1.setQuantity(100);
        item1.setSellingPrice(90.0);

        OrderForm orderForm = new OrderForm();
        orderForm.setCustomerPhone("9876543210");
        orderForm.setItems(Arrays.asList(item1));

        // WHEN / THEN
        // This is a flow-level exception, not a validation one.
        ApiException ex = assertThrows(ApiException.class,
                () -> orderDto.add(orderForm)
        );
        assertTrue(ex.getMessage().contains("Not enough stock is available for product " + product1.getName()));
    }

    // --- updateById() Tests ---

    @Test
    public void updateByIdValidDataShouldUpdatePhone() throws ApiException {
        // GIVEN
        OrderData addedOrder = orderDto.add(createValidOrderForm());
        OrderUpdateForm updateForm = new OrderUpdateForm();
        // Set a valid phone number (no spaces) for the update
        updateForm.setCustomerPhone("1111122222");
        updateForm.setOrderStatus(OrderStatus.CREATED);
        // Note: Dto.normalize() will be called *after* validation

        // WHEN
        OrderData updatedOrder = orderDto.updateById(addedOrder.getId(), updateForm);

        // THEN
        assertNotNull(updatedOrder);
        assertEquals(addedOrder.getId(), updatedOrder.getId());
        assertEquals("1111122222", updatedOrder.getCustomerPhone()); // Check normalized update

        OrderData fetchedOrder = orderDto.getById(addedOrder.getId());
        assertEquals("1111122222", fetchedOrder.getCustomerPhone());
    }

    @Test
    public void updateByIdInvalidPhoneShouldThrowException() throws ApiException {
        // GIVEN
        OrderData addedOrder = orderDto.add(createValidOrderForm());
        OrderUpdateForm updateForm = new OrderUpdateForm();
        updateForm.setCustomerPhone("invalid-phone"); // Fails @Pattern
        updateForm.setOrderStatus(OrderStatus.CREATED);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderDto.updateById(addedOrder.getId(), updateForm)
        );

        // --- THIS IS THE FIX ---
        // Assuming OrderUpdateForm has the same validation as OrderForm
        assertTrue("Exception was not a FormValidationException",
                ex instanceof FormValidationException);

        FormValidationException validationEx = (FormValidationException) ex;
        Map<String, String> errors = validationEx.getErrors();

        String expectedField = "customerPhone";
        String expectedMessage = "Phone number must contain only digits"; // Assuming same message

        assertEquals("The errors map should have one entry", 1, errors.size());
        assertTrue("Errors map does not contain key: " + expectedField,
                errors.containsKey(expectedField));
        assertEquals("Incorrect validation message for " + expectedField,
                expectedMessage, errors.get(expectedField));
    }

    @Test
    public void updateByIdNonExistingShouldThrowException() {
        // GIVEN
        OrderUpdateForm updateForm = new OrderUpdateForm();
        updateForm.setCustomerPhone("1234567890");
        updateForm.setOrderStatus(OrderStatus.CREATED);
        // WHEN / THEN
        // This is a flow-level exception
        ApiException ex = assertThrows(ApiException.class,
                () -> orderDto.updateById(99999, updateForm) // Non-existent ID
        );
        assertEquals("Order 99999 doesn't exist", ex.getMessage());
    }

    // --- getById() Tests ---

    @Test
    public void getByIdValidIdShouldReturnOrder() throws ApiException {
        // GIVEN
        OrderData addedOrder = orderDto.add(createValidOrderForm());

        // WHEN
        OrderData fetchedOrder = orderDto.getById(addedOrder.getId());

        // THEN
        assertNotNull(fetchedOrder);
        assertEquals(addedOrder.getId(), fetchedOrder.getId());
        assertEquals("9876543210", fetchedOrder.getCustomerPhone());
        assertEquals(2, fetchedOrder.getOrderItemDataList().size());

        String firstItemName = fetchedOrder.getOrderItemDataList().get(0).getProductName();
        assertTrue(firstItemName.equals("Product A") || firstItemName.equals("Product B"));
    }

    @Test
    public void getByIdNonExistingShouldThrowException() {
        // GIVEN: A non-existent ID

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> orderDto.getById(99999) // Non-existent ID
        );
        assertEquals("Order 99999 doesn't exist", ex.getMessage());
    }

    // --- getFilteredOrders() Tests ---

    @Test
    public void getFilteredOrdersAllShouldReturnAllPaginated() throws ApiException {
        // GIVEN
        OrderData order1 = orderDto.add(createValidOrderForm());
        OrderData order2 = orderDto.add(createValidOrderForm());

        // WHEN
        PaginationData<OrderData> result = orderDto.getFilteredOrders(null, null, null, null, 0, 10);

        // THEN
        assertEquals(Long.valueOf(2), result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(order2.getId(), result.getContent().get(0).getId());
        assertEquals(order1.getId(), result.getContent().get(1).getId());
    }

    @Test
    public void getFilteredOrdersByOrderIdShouldReturnOne() throws ApiException {
        // GIVEN
        OrderData order1 = orderDto.add(createValidOrderForm());
        orderDto.add(createValidOrderForm()); // order2

        // WHEN
        PaginationData<OrderData> result = orderDto.getFilteredOrders(order1.getId(), null, null, null, 0, 10);

        // THEN
        assertEquals(Long.valueOf(1), result.getTotalElements());
        assertEquals(order1.getId(), result.getContent().get(0).getId());
    }

    @Test
    public void getFilteredOrdersByStatusShouldReturnFiltered() throws ApiException {
        // GIVEN
        orderDto.add(createValidOrderForm());
        orderDto.add(createValidOrderForm());

        // WHEN
        PaginationData<OrderData> resultCreated = orderDto.getFilteredOrders(null, null, null, OrderStatus.CREATED, 0, 10);

        // THEN
        assertEquals(Long.valueOf(2), resultCreated.getTotalElements());

        // WHEN
        PaginationData<OrderData> resultInvoiced = orderDto.getFilteredOrders(null, null, null, OrderStatus.INVOICED, 0, 10);

        // THEN
        assertEquals(Long.valueOf(0), resultInvoiced.getTotalElements());
    }

    @Test
    public void getFilteredOrdersByDateShouldReturnFiltered() throws ApiException {
        // GIVEN
        OrderData order1 = orderDto.add(createValidOrderForm());

        // WHEN
        ZonedDateTime startDate = ZonedDateTime.now().minusDays(1);
        ZonedDateTime endDate = ZonedDateTime.now().plusDays(1);
        PaginationData<OrderData> result = orderDto.getFilteredOrders(null, startDate, endDate, null, 0, 10);

        // THEN
        assertEquals(Long.valueOf(1), result.getTotalElements());
        assertEquals(order1.getId(), result.getContent().get(0).getId());

        // WHEN
        ZonedDateTime futureStartDate = ZonedDateTime.now().plusDays(1);
        ZonedDateTime futureEndDate = ZonedDateTime.now().plusDays(2);
        PaginationData<OrderData> futureResult = orderDto.getFilteredOrders(null, futureStartDate, futureEndDate, null, 0, 10);

        // THEN
        assertEquals(Long.valueOf(0), futureResult.getTotalElements());
    }

    @Test
    public void getFilteredOrdersPaginationShouldWork() throws ApiException {
        // GIVEN
        OrderData order1 = orderDto.add(createValidOrderForm());
        OrderData order2 = orderDto.add(createValidOrderForm());
        OrderData order3 = orderDto.add(createValidOrderForm());

        // WHEN: Get first page, size 2 (Sort is ID descending)
        PaginationData<OrderData> page1 = orderDto.getFilteredOrders(null, null, null, null, 0, 2);

        // THEN
        assertEquals(Long.valueOf(3), page1.getTotalElements());
        assertEquals(Integer.valueOf(2), page1.getTotalPages());
        assertEquals(2, page1.getContent().size());
        assertEquals(order3.getId(), page1.getContent().get(0).getId());
        assertEquals(order2.getId(), page1.getContent().get(1).getId());

        // WHEN: Get second page, size 2
        PaginationData<OrderData> page2 = orderDto.getFilteredOrders(null, null, null, null, 1, 2);

        // THEN
        assertEquals(Long.valueOf(3), page2.getTotalElements());
        assertEquals(Integer.valueOf(2), page2.getTotalPages());
        assertEquals(1, page2.getContent().size());
        assertEquals(order1.getId(), page2.getContent().get(0).getId());
    }
}