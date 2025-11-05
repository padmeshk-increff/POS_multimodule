package com.increff.pos.unit.flow;

import com.increff.pos.api.InvoiceApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.enums.OrderStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static com.increff.pos.factory.OrderFactory.mockPersistedObject;
import static com.increff.pos.factory.OrderItemFactory.mockPersistedObject;
import static com.increff.pos.factory.ProductFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Behavior-focused unit tests for InvoiceFlow.
 * Complex invoice generation logic tested via integration tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class InvoiceFlowTest {

    @Mock
    private OrderApi orderApi;
    @Mock
    private OrderItemApi orderItemApi;
    @Mock
    private ProductApi productApi;
    @Mock
    private InvoiceApi invoiceApi;
    @InjectMocks
    private InvoiceFlow invoiceFlow;

    private final Integer ORDER_ID = 1;
    private Order mockOrder;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(invoiceFlow, "invoiceStoragePath", "/test-path");
        mockOrder = mockPersistedObject(ORDER_ID, OrderStatus.INVOICED);
    }

    // NOTE: Invoice generation involves complex file I/O and helpers.
    // Full flow better tested via integration tests.
    // Here we test only critical error paths.
    
    @Test
    public void generateInvoiceFormInvoiceAlreadyExistsShouldThrowException() throws ApiException {
        // GIVEN
        doThrow(new ApiException("Invoice already exists"))
                .when(invoiceApi).checkInvoiceDoesNotExist(ORDER_ID);

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceFlow.generateInvoiceForm(ORDER_ID)
        );
        assertEquals("Invoice already exists", ex.getMessage());
    }

    @Test
    public void generateInvoiceFormOrderNotInCreatedStateShouldThrowException() throws ApiException {
        // GIVEN
        doNothing().when(invoiceApi).checkInvoiceDoesNotExist(ORDER_ID);
        when(orderApi.updateInvoiceOrder(ORDER_ID))
                .thenThrow(new ApiException("Only CREATED orders can be invoiced"));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceFlow.generateInvoiceForm(ORDER_ID)
        );
        assertEquals("Only CREATED orders can be invoiced", ex.getMessage());
    }
}