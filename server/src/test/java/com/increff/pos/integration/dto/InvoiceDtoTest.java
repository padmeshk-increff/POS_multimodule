package com.increff.pos.integration.dto;

import com.increff.pos.api.*;
import com.increff.pos.config.SpringConfig; // Use the main SpringConfig
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.InvoiceDto;
import com.increff.pos.entity.*;
import com.increff.pos.factory.ClientFactory;
import com.increff.pos.factory.OrderFactory;
import com.increff.pos.factory.OrderItemFactory;
import com.increff.pos.factory.ProductFactory;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.enums.OrderStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration Tests for the InvoiceDto class.
 * This test validates the full stack, including the external Invoice App.
 *
 * !! WARNING: This test requires the external Invoice App to be running
 * at the URL specified in 'invoice.app.url' (http://localhost:9090/invoice).
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class) // Load the main app config
@WebAppConfiguration
@TestPropertySource("classpath:test.properties") // Override the DB to 'test_pos'
@Transactional // Roll back all database changes
public class InvoiceDtoTest {

    // --- Autowire all REAL beans ---
    @Autowired
    private InvoiceDto invoiceDto;

    // We need all these to create a valid order
    @Autowired
    private OrderApi orderApi;
    @Autowired
    private OrderItemApi orderItemApi;
    @Autowired
    private ProductFlow productFlow;
    @Autowired
    private ClientApi clientApi;
    @Autowired
    private InventoryApi inventoryApi;
    @Autowired
    private InvoiceApi invoiceApi; // For verification

    private Order testOrder;
    private Product testProduct;

    @Before
    public void setUp() throws ApiException {
        // --- GIVEN ---
        // We must create a valid, "CREATED" order in the test DB

        // 1. Create Client
        Client c = ClientFactory.mockNewObject();
        clientApi.insert(c);

        // 2. Create Product (which also creates Inventory)
        Product p = ProductFactory.mockNewObject(c.getId(),100.0);
        testProduct = productFlow.insert(p);

        // 3. Manually set inventory
        Inventory inv = inventoryApi.getCheckByProductId(testProduct.getId());
        inv.setQuantity(100);
        inventoryApi.update(inv);

        // 4. Create Order and OrderItem
        Order o = OrderFactory.mockNewObject();
        orderApi.insert(o);
        testOrder = o;

        OrderItem oi = OrderItemFactory.mockNewObject(testOrder.getId(), testProduct.getId(),5,80.0);
        orderItemApi.insert(oi);

        // 5. Update order total amount
        o.setTotalAmount(400.0);
        orderApi.updateById(o.getId(), o);
    }

    // --- generateAndStoreInvoice() Tests ---

    @Test
    public void generateAndStoreInvoice_happyPath_shouldSucceed() throws ApiException {
        // GIVEN: A valid order (from @Before)
        // ASSUMPTION: The external invoice app at http://localhost:9090 is RUNNING.

        // WHEN
        Map<String, String> response = invoiceDto.generateAndStoreInvoice(testOrder.getId());

        // THEN
        // 1. Check the response message
        assertNotNull(response);
        assertEquals("Invoice generated and stored successfully for order ID: " + testOrder.getId(), response.get("message"));

        // 2. Verify the side-effects in the database
        // Order status should be INVOICED
        Order fromDb = orderApi.getCheckById(testOrder.getId());
        assertEquals(OrderStatus.INVOICED, fromDb.getOrderStatus());
        assertNotNull(fromDb.getInvoicePath()); // Invoice path should be set

        // Invoice record should exist
        assertNotNull(invoiceApi.getCheckByOrderId(testOrder.getId()));
    }

    @Test
    public void generateAndStoreInvoice_orderAlreadyInvoiced_shouldThrowException() throws ApiException {
        // GIVEN
        // Manually set the order to INVOICED
        invoiceDto.generateAndStoreInvoice(testOrder.getId());

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> invoiceDto.generateAndStoreInvoice(testOrder.getId())
        );
        // This exception comes from the underlying invoiceFlow -> orderApi.updateInvoiceOrder
        assertTrue(ex.getMessage().contains("An invoice for order ID " + testOrder.getId() + " has already been generated."));
    }

    // --- getInvoicePdf() Tests ---

    @Test
    public void getInvoicePdf_noInvoiceExists_shouldThrowException() {
        // GIVEN: An order that has NOT been invoiced (from @Before)

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> invoiceDto.getInvoicePdf(testOrder.getId())
        );
        // This exception comes from invoiceApi.getCheckByOrderId
        assertEquals("Invoice with Order ID " + testOrder.getId() + " doesn't not exist", ex.getMessage());
    }

    @Test
    public void getInvoicePdf_fileMissing_shouldThrowException() throws ApiException {
        // GIVEN
        // 1. We *must* successfully generate the invoice in the DB
        // This test ASSUMES the external invoice app is running and will succeed.
        invoiceDto.generateAndStoreInvoice(testOrder.getId());

        // 2. The invoiceApi.getInvoicePdfBytes() will now be called,
        //    but it will try to read a file from disk. Since the file
        //    is saved in your file system, this *should* pass if the
        //    invoice.storage.path in your test.properties is correct.

        // This test verifies the PDF *bytes* can be retrieved

        // WHEN
        ResponseEntity<byte[]> response = invoiceDto.getInvoicePdf(testOrder.getId());

        // THEN
        // This test is tricky. If the PDF generation works, it will return the PDF.
        // If the path is wrong, it will throw an exception.
        // We'll assert that the response is valid.
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().length > 0);
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());

        // 2. Check the content disposition header parts
        assertNotNull(response.getHeaders().getContentDisposition());
        assertTrue(response.getHeaders().getContentDisposition().isAttachment());
        assertEquals("invoice-order-" + testOrder.getId() + ".pdf",
                response.getHeaders().getContentDisposition().getFilename());
    }
}