package com.increff.pos.unit.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.OrderItemFlow;
import com.increff.pos.model.enums.OrderStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.increff.pos.factory.OrderFactory.mockPersistedObject;
import static com.increff.pos.factory.OrderItemFactory.mockNewObject;
import static com.increff.pos.factory.OrderItemFactory.mockPersistedObject;
import static com.increff.pos.factory.ProductFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Behavior-focused unit tests for OrderItemFlow.
 * Tests order item modification rules and inventory updates.
 */
public class OrderItemFlowTest {

    @Mock
    private OrderApi orderApi;
    @Mock
    private OrderItemApi orderItemApi;
    @Mock
    private ProductApi productApi;
    @Mock
    private InventoryApi inventoryApi;
    @InjectMocks
    private OrderItemFlow orderItemFlow;

    private Order mutableOrder;
    private Order invoicedOrder;
    private Product existingProduct;
    private OrderItem existingOrderItem;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mutableOrder = mockPersistedObject(1, OrderStatus.CREATED);
        invoicedOrder = mockPersistedObject(2, OrderStatus.INVOICED);

        existingProduct = mockPersistedObject(101, "barcode-101", 1);
        existingProduct.setMrp(100.0);

        existingOrderItem = mockPersistedObject(501, 1, 101);
        existingOrderItem.setQuantity(5);
        existingOrderItem.setSellingPrice(80.0);
    }

    // NOTE: OrderItem logic with inventory/price updates is complex.
    // Full behavior better tested via integration tests.
    
    @Test
    public void addOrderIsInvoicedShouldThrowException() throws ApiException {
        // GIVEN
        OrderItem newItem = mockNewObject(2, 101);
        when(orderApi.getCheckById(2)).thenReturn(invoicedOrder);

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemFlow.add(newItem)
        );
        assertTrue(ex.getMessage().contains("Cannot modify"));
    }

    @Test
    public void addSellingPriceExceedsMrpShouldThrowException() throws ApiException {
        // GIVEN
        OrderItem newItem = mockNewObject(1, 101);
        newItem.setSellingPrice(101.0); // MRP is 100.0
        when(orderApi.getCheckById(1)).thenReturn(mutableOrder);
        when(productApi.getCheckById(101)).thenReturn(existingProduct);

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemFlow.add(newItem)
        );
        assertTrue(ex.getMessage().contains("cannot be greater than its MRP"));
    }

    @Test
    public void addNotEnoughStockShouldThrowException() throws ApiException {
        // GIVEN
        OrderItem newItem = mockNewObject(1, 101);
        newItem.setSellingPrice(90.0);
        newItem.setQuantity(10);
        when(orderApi.getCheckById(1)).thenReturn(mutableOrder);
        when(productApi.getCheckById(101)).thenReturn(existingProduct);
        doThrow(new ApiException("Not enough stock")).when(inventoryApi).updateQuantityByProductId(101, 0, 10);

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemFlow.add(newItem)
        );
        assertEquals("Not enough stock", ex.getMessage());
    }

    @Test
    public void updateValidUpdateShouldUpdateInventoryAndPrice() throws ApiException {
        // GIVEN
        OrderItem updateData = new OrderItem();
        updateData.setId(501);
        updateData.setOrderId(1);
        updateData.setProductId(101);
        updateData.setQuantity(10); // Old: 5
        updateData.setSellingPrice(90.0); // Old: 80.0
        
        when(orderApi.getCheckById(1)).thenReturn(mutableOrder);
        when(orderItemApi.getCheckById(501)).thenReturn(existingOrderItem);
        when(productApi.getCheckById(101)).thenReturn(existingProduct);
        when(orderItemApi.update(any())).thenReturn(updateData);

        // WHEN
        OrderItem result = orderItemFlow.update(updateData);

        // THEN - Verify critical side effects
        assertNotNull(result);
        verify(inventoryApi).updateQuantityByProductId(101, 5, 10);
        verify(orderApi).updateAmountById(1, 400.0, 900.0);
    }

    @Test
    public void updateItemNotFoundShouldThrowException() throws ApiException {
        // GIVEN
        OrderItem updateData = new OrderItem();
        updateData.setId(999);
        updateData.setOrderId(1);
        when(orderApi.getCheckById(1)).thenReturn(mutableOrder);
        when(orderItemApi.getCheckById(999)).thenThrow(new ApiException("Item not found"));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemFlow.update(updateData)
        );
        assertEquals("Item not found", ex.getMessage());
    }

    @Test
    public void deleteByIdValidDeleteShouldRestockInventory() throws ApiException {
        // GIVEN
        when(orderApi.getCheckById(1)).thenReturn(mutableOrder);
        when(orderItemApi.getCheckById(501)).thenReturn(existingOrderItem);

        // WHEN
        orderItemFlow.deleteById(1, 501);

        // THEN - Verify critical side effects
        verify(inventoryApi).updateQuantityByProductId(101, 5, 0);
        verify(orderApi).updateAmountById(1, 400.0, 0.00);
        verify(orderItemApi).deleteById(501, 1);
    }

    @Test
    public void deleteByIdOrderIsInvoicedShouldThrowException() throws ApiException {
        // GIVEN
        when(orderApi.getCheckById(2)).thenReturn(invoicedOrder);

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemFlow.deleteById(2, 501)
        );
        assertTrue(ex.getMessage().contains("Cannot modify"));
    }

    @Test
    public void deleteByIdItemNotFoundShouldThrowException() throws ApiException {
        // GIVEN
        when(orderApi.getCheckById(1)).thenReturn(mutableOrder);
        when(orderItemApi.getCheckById(999)).thenThrow(new ApiException("Item not found"));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemFlow.deleteById(1, 999)
        );
        assertEquals("Item not found", ex.getMessage());
    }
}