package com.increff.pos.unit.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.factory.InventoryFactory;
import com.increff.pos.factory.ProductFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static com.increff.pos.factory.OrderFactory.mockNewObject;
import static com.increff.pos.factory.OrderFactory.mockPersistedObject;
import static com.increff.pos.factory.OrderItemFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Behavior-focused unit tests for OrderFlow.
 * Complex order logic tested via integration tests.
 */
public class OrderFlowTest {

    @Mock
    private OrderApi orderApi;
    @Mock
    private ProductApi productApi;
    @Mock
    private InventoryApi inventoryApi;
    @Mock
    private OrderItemApi orderItemApi;
    @InjectMocks
    private OrderFlow orderFlow;

    private Order mockOrder;
    private List<OrderItem> mockItems;
    private Inventory mockInventory1;
    private Inventory mockInventory2;
    private Product mockProduct1;
    private Product mockProduct2;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockOrder = mockNewObject();
        mockOrder.setId(1);

        OrderItem mockItem1 = mockPersistedObject(1, 1, 101);
        mockItem1.setQuantity(5);
        mockItem1.setSellingPrice(100.0);

        OrderItem mockItem2 = mockPersistedObject(2, 1, 102);
        mockItem2.setQuantity(2);
        mockItem2.setSellingPrice(50.0);

        mockItems = Arrays.asList(mockItem1, mockItem2);

        mockInventory1 = InventoryFactory.mockPersistedObject(101, 50);
        mockInventory2 = InventoryFactory.mockPersistedObject(102, 20);

        mockProduct1 = ProductFactory.mockPersistedObject(101);
        mockProduct1.setMrp(120.0);
        mockProduct2 = ProductFactory.mockPersistedObject(102);
        mockProduct2.setMrp(50.0);
    }

    // NOTE: Order logic is complex with inventory checks, price validation, etc.
    // Full flow better tested via integration tests.
    
    @Test
    public void insertNotEnoughStockShouldThrowException() throws ApiException {
        // GIVEN
        when(orderApi.insert(any())).thenReturn(mockOrder);
        mockInventory1.setQuantity(3); // Need 5, only have 3
        when(inventoryApi.getByProductIds(any())).thenReturn(Arrays.asList(mockInventory1, mockInventory2));
        when(productApi.getByIds(any())).thenReturn(Arrays.asList(mockProduct1, mockProduct2));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderFlow.insert(mockOrder, mockItems)
        );
        assertTrue(ex.getMessage().contains("Not enough stock"));
    }

    @Test
    public void insertSellingPriceExceedsMrpShouldThrowException() throws ApiException {
        // GIVEN
        when(orderApi.insert(any())).thenReturn(mockOrder);
        when(inventoryApi.getByProductIds(any())).thenReturn(Arrays.asList(mockInventory1, mockInventory2));
        mockProduct1.setMrp(90.0); // SP is 100, MRP is 90
        when(productApi.getByIds(any())).thenReturn(Arrays.asList(mockProduct1, mockProduct2));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderFlow.insert(mockOrder, mockItems)
        );
        assertTrue(ex.getMessage().contains("Selling price cannot be more than mrp"));
    }

    @Test
    public void getByIdValidIdShouldReturnOrderResult() throws ApiException {
        // GIVEN
        Order existingOrder = mockPersistedObject(1, OrderStatus.CREATED);
        when(orderApi.getCheckById(1)).thenReturn(existingOrder);
        when(orderItemApi.getAllByOrderId(1)).thenReturn(mockItems);

        // WHEN
        OrderResult result = orderFlow.getById(1);

        // THEN
        assertNotNull(result);
        assertEquals(existingOrder, result.getOrder());
        assertEquals(mockItems, result.getOrderItems());
    }

    @Test
    public void getByIdOrderNotFoundShouldThrowException() throws ApiException {
        // GIVEN
        when(orderApi.getCheckById(999)).thenThrow(new ApiException("Order 999 doesn't exist"));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderFlow.getById(999)
        );
        assertEquals("Order 999 doesn't exist", ex.getMessage());
    }

    @Test
    public void updateByIdToCancelledShouldRestockInventory() throws ApiException {
        // GIVEN
        Order orderUpdate = new Order();
        orderUpdate.setOrderStatus(OrderStatus.CANCELLED);
        Order updatedOrder = mockPersistedObject(1, OrderStatus.CANCELLED);
        when(orderApi.updateById(1, orderUpdate)).thenReturn(updatedOrder);
        when(orderItemApi.getAllByOrderId(1)).thenReturn(mockItems);

        // WHEN
        OrderResult result = orderFlow.updateById(1, orderUpdate);

        // THEN - Verify critical side effect: inventory restocked
        assertNotNull(result);
        verify(inventoryApi).updateQuantityByProductId(101, 5, 0);
        verify(inventoryApi).updateQuantityByProductId(102, 2, 0);
    }

    @Test
    public void updateByIdToCreatedShouldNotRestockInventory() throws ApiException {
        // GIVEN
        Order orderUpdate = new Order();
        orderUpdate.setOrderStatus(OrderStatus.CREATED);
        Order updatedOrder = mockPersistedObject(1, OrderStatus.CREATED);
        when(orderApi.updateById(1, orderUpdate)).thenReturn(updatedOrder);
        when(orderItemApi.getAllByOrderId(1)).thenReturn(mockItems);

        // WHEN
        OrderResult result = orderFlow.updateById(1, orderUpdate);

        // THEN - Verify NO restock
        assertNotNull(result);
        verify(inventoryApi, never()).updateQuantityByProductId(any(Integer.class), any(Integer.class), any(Integer.class));
    }

    @Test
    public void updateByIdOrderUpdateFailsShouldThrowException() throws ApiException {
        // GIVEN
        Order orderUpdate = new Order();
        orderUpdate.setOrderStatus(OrderStatus.CANCELLED);
        when(orderApi.updateById(1, orderUpdate))
                .thenThrow(new ApiException("Cannot update an order that has already been invoiced"));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> orderFlow.updateById(1, orderUpdate)
        );
        assertEquals("Cannot update an order that has already been invoiced", ex.getMessage());
    }
}