package com.increff.pos.unit.api;

import com.increff.pos.api.OrderApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.OrderDao;
import com.increff.pos.entity.Order;
import com.increff.pos.model.enums.OrderStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static com.increff.pos.factory.OrderFactory.mockNewObject;
import static com.increff.pos.factory.OrderFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderApiTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private Pageable mockPageable;

    @InjectMocks
    private OrderApi orderApi;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------------------------------------------------------
    // insert()
    // ---------------------------------------------------------------------

    @Test
    public void insertValidOrderReturnsSameInstance() throws ApiException {
        Order newOrder = mockNewObject();

        Order saved = orderApi.insert(newOrder);

        assertSame(newOrder, saved);
    }

    @Test
    public void insertNullOrderThrowsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.insert(null)
        );
        assertEquals("Order object cannot be null", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getAll()
    // ---------------------------------------------------------------------

    @Test
    public void getAllReturnsOrdersFromDao() {
        List<Order> expected = Collections.singletonList(mockPersistedObject());
        when(orderDao.selectAll()).thenReturn(expected);

        List<Order> result = orderApi.getAll();

        assertEquals(expected, result);
    }

    // ---------------------------------------------------------------------
    // getCheckById()
    // ---------------------------------------------------------------------

    @Test
    public void getCheckByIdExistingIdReturnsOrder() throws ApiException {
        Order existing = mockPersistedObject(1, OrderStatus.CREATED);
        when(orderDao.selectById(1)).thenReturn(existing);

        Order result = orderApi.getCheckById(1);

        assertSame(existing, result);
    }

    @Test
    public void getCheckByIdNullIdThrowsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.getCheckById(null)
        );
        assertEquals("Id cannot null", ex.getMessage());
    }

    @Test
    public void getCheckByIdMissingIdThrowsException() {
        when(orderDao.selectById(404)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.getCheckById(404)
        );
        assertEquals("Order 404 doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // updateInvoicePathById()
    // ---------------------------------------------------------------------

    @Test
    public void updateInvoicePathByIdExistingOrderUpdatesPath() throws ApiException {
        Order existing = mockPersistedObject(2, OrderStatus.CREATED);
        when(orderDao.selectById(2)).thenReturn(existing);

        Order result = orderApi.updateInvoicePathById(2, "/invoices/2.pdf");

        assertSame(existing, result);
        assertEquals("/invoices/2.pdf", existing.getInvoicePath());
    }

    @Test
    public void updateInvoicePathByIdNullIdThrowsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.updateInvoicePathById(null, "path")
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    @Test
    public void updateInvoicePathByIdNullPathThrowsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.updateInvoicePathById(1, null)
        );
        assertEquals("File Path cannot be null", ex.getMessage());
    }

    @Test
    public void updateInvoicePathByIdMissingOrderThrowsException() {
        when(orderDao.selectById(7)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.updateInvoicePathById(7, "path")
        );
        assertEquals("Order 7 doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // updateById()
    // ---------------------------------------------------------------------

    @Test
    public void updateByIdCreatedOrderAppliesChanges() throws ApiException {
        Order existing = mockPersistedObject(5, OrderStatus.CREATED);
        existing.setCustomerName("Old");
        when(orderDao.selectById(5)).thenReturn(existing);

        Order updates = new Order();
        updates.setOrderStatus(OrderStatus.CANCELLED);
        updates.setCustomerName("New");

        Order result = orderApi.updateById(5, updates);

        assertSame(existing, result);
        assertEquals(OrderStatus.CANCELLED, existing.getOrderStatus());
        assertEquals("New", existing.getCustomerName());
    }

    @Test
    public void updateByIdInvoicedOrderThrowsException() {
        Order existing = mockPersistedObject(6, OrderStatus.INVOICED);
        when(orderDao.selectById(6)).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.updateById(6, new Order())
        );
        assertEquals("Cannot update an order that has already been invoiced", ex.getMessage());
    }

    @Test
    public void updateByIdCancelledOrderThrowsException() {
        Order existing = mockPersistedObject(6, OrderStatus.CANCELLED);
        when(orderDao.selectById(6)).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.updateById(6, new Order())
        );
        assertEquals("Cannot update an order that as been cancelled", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // updateInvoiceOrder()
    // ---------------------------------------------------------------------

    @Test
    public void updateInvoiceOrderCreatedOrderBecomesInvoiced() throws ApiException {
        Order existing = mockPersistedObject(8, OrderStatus.CREATED);
        when(orderDao.selectById(8)).thenReturn(existing);

        Order result = orderApi.updateInvoiceOrder(8);

        assertSame(existing, result);
        assertEquals(OrderStatus.INVOICED, existing.getOrderStatus());
    }

    @Test
    public void updateInvoiceOrderInvalidStatusThrowsException() {
        Order existing = mockPersistedObject(9, OrderStatus.INVOICED);
        when(orderDao.selectById(9)).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.updateInvoiceOrder(9)
        );
        assertEquals("Only an order with status CREATED can be invoiced. Current status: INVOICED", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // updateAmountById()
    // ---------------------------------------------------------------------

    @Test
    public void updateAmountByIdValidInputAdjustsTotal() throws ApiException {
        Order existing = mockPersistedObject(10, OrderStatus.CREATED, 100.0);
        when(orderDao.selectById(10)).thenReturn(existing);

        orderApi.updateAmountById(10, 20.0, 35.0);

        assertEquals(115.0, existing.getTotalAmount(), 0.001);
    }

    @Test
    public void updateAmountByIdNullArgumentsThrowExceptions() {
        ApiException idEx = assertThrows(ApiException.class,
            () -> orderApi.updateAmountById(null, 1.0, 2.0)
        );
        assertEquals("Order id cannot be null", idEx.getMessage());

        ApiException oldEx = assertThrows(ApiException.class,
            () -> orderApi.updateAmountById(1, null, 2.0)
        );
        assertEquals("Old SP can't be null", oldEx.getMessage());

        ApiException newEx = assertThrows(ApiException.class,
            () -> orderApi.updateAmountById(1, 2.0, null)
        );
        assertEquals("New SP cannot be null", newEx.getMessage());
    }

    // ---------------------------------------------------------------------
    // deleteById()
    // ---------------------------------------------------------------------

    @Test
    public void deleteByIdExistingOrderDeletes() throws ApiException {
        Order existing = mockPersistedObject(11, OrderStatus.CREATED);
        when(orderDao.selectById(11)).thenReturn(existing);

        orderApi.deleteById(11);

        verify(orderDao).deleteById(11);
    }

    @Test
    public void deleteByIdMissingOrderThrowsException() {
        when(orderDao.selectById(12)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.deleteById(12)
        );
        assertEquals("Order doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getByFilters()
    // ---------------------------------------------------------------------

    @Test
    public void getByFiltersValidParametersDelegatesToDao() throws ApiException {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusDays(1);
        when(orderDao.findWithFilters(1, start, end, OrderStatus.CREATED, mockPageable))
            .thenReturn(Collections.emptyList());

        List<Order> result = orderApi.getByFilters(1, start, end, OrderStatus.CREATED, mockPageable);

        assertNotNull(result);
    }

    @Test
    public void getByFiltersNullPageableThrowsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.getByFilters(1, null, null, null, null)
        );
        assertEquals("Pageable cannot be null", ex.getMessage());
    }

    @Test
    public void getByFiltersInvalidRangeThrowsException() {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.minusDays(1);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.getByFilters(1, start, end, null, mockPageable)
        );
        assertEquals("Start date cannot be after end date.", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // countWithFilters()
    // ---------------------------------------------------------------------

    @Test
    public void countWithFiltersValidParametersReturnsCount() throws ApiException {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusDays(1);
        when(orderDao.countWithFilters(1, start, end, OrderStatus.CREATED)).thenReturn(5L);

        Long count = orderApi.countWithFilters(1, start, end, OrderStatus.CREATED);

        assertEquals(Long.valueOf(5L), count);
    }

    @Test
    public void countWithFiltersInvalidRangeThrowsException() {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.minusDays(1);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.countWithFilters(1, start, end, null)
        );
        assertEquals("Start date cannot be after end date.", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getAllByDateRange()
    // ---------------------------------------------------------------------

    @Test
    public void getAllByDateRangeValidRangeReturnsOrders() throws ApiException {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusDays(1);
        when(orderDao.selectAllByDateRange(start, end)).thenReturn(Collections.emptyList());

        List<Order> result = orderApi.getAllByDateRange(start, end);

        assertNotNull(result);
    }

    @Test
    public void getAllByDateRangeNullDatesThrowExceptions() {
        ZonedDateTime now = ZonedDateTime.now();

        ApiException startEx = assertThrows(ApiException.class,
            () -> orderApi.getAllByDateRange(null, now)
        );
        assertEquals("Start date cannot be null", startEx.getMessage());

        ApiException endEx = assertThrows(ApiException.class,
            () -> orderApi.getAllByDateRange(now, null)
        );
        assertEquals("End date cannot be null", endEx.getMessage());
    }

    @Test
    public void getAllByDateRangeInvalidRangeThrowsException() {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.minusDays(1);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderApi.getAllByDateRange(start, end)
        );
        assertEquals("Start date cannot be after end date.", ex.getMessage());
    }
}