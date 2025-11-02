package com.increff.pos.unit.api;

import com.increff.pos.api.OrderItemApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.OrderItemDao;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.result.ProductQuantityResult;
import com.increff.pos.model.result.SalesOverTimeResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.increff.pos.factory.OrderItemFactory.mockNewObject;
import static com.increff.pos.factory.OrderItemFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderItemApiTest {

    @Mock
    private OrderItemDao orderItemDao;

    @InjectMocks
    private OrderItemApi orderItemApi;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------------------------------------------------------
    // getAll()
    // ---------------------------------------------------------------------

    @Test
    public void getAll_returnsDaoResults() {
        List<OrderItem> expected = Collections.singletonList(mockPersistedObject());
        when(orderItemDao.selectAll()).thenReturn(expected);

        List<OrderItem> result = orderItemApi.getAll();

        assertEquals(expected, result);
    }

    // ---------------------------------------------------------------------
    // getByOrderIds()
    // ---------------------------------------------------------------------

    @Test
    public void getByOrderIds_withIds_returnsItems() throws ApiException {
        List<OrderItem> expected = Collections.singletonList(mockPersistedObject());
        when(orderItemDao.selectByOrderIds(Collections.singletonList(1))).thenReturn(expected);

        List<OrderItem> result = orderItemApi.getByOrderIds(Collections.singletonList(1));

        assertEquals(expected, result);
    }

    @Test
    public void getByOrderIds_nullList_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.getByOrderIds(null)
        );
        assertEquals("order Ids cannot be null", ex.getMessage());
    }

    @Test
    public void getByOrderIds_emptyList_returnsEmptyList() throws ApiException {
        List<OrderItem> result = orderItemApi.getByOrderIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(orderItemDao, never()).selectByOrderIds(Collections.emptyList());
    }

    // ---------------------------------------------------------------------
    // getAllByOrderId()
    // ---------------------------------------------------------------------

    @Test
    public void getAllByOrderId_validId_returnsItems() throws ApiException {
        List<OrderItem> expected = Collections.singletonList(mockPersistedObject());
        when(orderItemDao.selectByOrderId(2)).thenReturn(expected);

        List<OrderItem> result = orderItemApi.getAllByOrderId(2);

        assertEquals(expected, result);
    }

    @Test
    public void getAllByOrderId_nullId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.getAllByOrderId(null)
        );
        assertEquals("Order id cannot be null", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getCheckById()
    // ---------------------------------------------------------------------

    @Test
    public void getCheckById_existingId_returnsItem() throws ApiException {
        OrderItem existing = mockPersistedObject();
        when(orderItemDao.selectById(existing.getId())).thenReturn(existing);

        OrderItem result = orderItemApi.getCheckById(existing.getId());

        assertSame(existing, result);
    }

    @Test
    public void getCheckById_nullId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.getCheckById(null)
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    @Test
    public void getCheckById_missingId_throwsException() {
        when(orderItemDao.selectById(404)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.getCheckById(404)
        );
        assertEquals("Order Item doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // insert()
    // ---------------------------------------------------------------------

    @Test
    public void insert_uniqueItem_returnsSameInstance() throws ApiException {
        OrderItem newItem = mockNewObject(1, 10);
        when(orderItemDao.selectByOrderIdAndProductId(1, 10)).thenReturn(null);

        OrderItem result = orderItemApi.insert(newItem);

        assertSame(newItem, result);
    }

    @Test
    public void insert_nullItem_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.insert(null)
        );
        assertEquals("Order Item cannot be null", ex.getMessage());
    }

    @Test
    public void insert_duplicateItem_throwsException() {
        OrderItem newItem = mockNewObject(1, 10);
        when(orderItemDao.selectByOrderIdAndProductId(1, 10)).thenReturn(mockPersistedObject());

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.insert(newItem)
        );
        assertEquals("Order Item already exists", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // insertAll()
    // ---------------------------------------------------------------------

    @Test
    public void insertAll_nonEmptyList_forwardsToDao() throws ApiException {
        List<OrderItem> items = Collections.singletonList(mockNewObject(1, 20));

        orderItemApi.insertAll(items);

        verify(orderItemDao).insertAll(items);
    }

    @Test
    public void insertAll_nullList_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.insertAll(null)
        );
        assertEquals("Order items list cannot be null", ex.getMessage());
    }

    @Test
    public void insertAll_emptyList_returnsSilently() throws ApiException {
        orderItemApi.insertAll(Collections.emptyList());

        verify(orderItemDao, never()).insertAll(Collections.emptyList());
    }

    // ---------------------------------------------------------------------
    // getTopSellingProducts()
    // ---------------------------------------------------------------------

    @Test
    public void getTopSellingProducts_withLimit_usesPagedQuery() throws ApiException {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusDays(1);
        Pageable pageable = PageRequest.of(0, 3);
        when(orderItemDao.findTopSellingProducts(start, end, pageable, OrderStatus.INVOICED))
            .thenReturn(Collections.singletonList(new ProductQuantityResult(1, 5L, 100.0)));

        List<ProductQuantityResult> result = orderItemApi.getTopSellingProducts(start, end, 3);

        assertEquals(1, result.size());
    }

    @Test
    public void getTopSellingProducts_noLimit_usesUnpagedQuery() throws ApiException {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusDays(1);
        when(orderItemDao.findTopSellingProducts(start, end, Pageable.unpaged(), OrderStatus.INVOICED))
            .thenReturn(Collections.emptyList());

        List<ProductQuantityResult> result = orderItemApi.getTopSellingProducts(start, end, null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getTopSellingProducts_missingDates_throwExceptions() {
        ZonedDateTime now = ZonedDateTime.now();
        ApiException startEx = assertThrows(ApiException.class,
            () -> orderItemApi.getTopSellingProducts(null, now, 1)
        );
        assertEquals("Start date cannot be null", startEx.getMessage());

        ApiException endEx = assertThrows(ApiException.class,
            () -> orderItemApi.getTopSellingProducts(now, null, 1)
        );
        assertEquals("End date cannot be null", endEx.getMessage());
    }

    @Test
    public void getTopSellingProducts_invalidRange_throwsException() {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.minusDays(1);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.getTopSellingProducts(start, end, 1)
        );
        assertEquals("Start date cannot be after end date.", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getSalesByDate()
    // ---------------------------------------------------------------------

    @Test
    public void getSalesByDate_validRange_returnsResults() throws ApiException {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusDays(1);
        when(orderItemDao.findSalesByDate(start, end, OrderStatus.INVOICED))
            .thenReturn(Collections.singletonList(new SalesOverTimeResult(new Date(), 100.0)));

        List<SalesOverTimeResult> result = orderItemApi.getSalesByDate(start, end);

        assertEquals(1, result.size());
    }

    @Test
    public void getSalesByDate_missingDates_throwExceptions() {
        ZonedDateTime now = ZonedDateTime.now();
        ApiException startEx = assertThrows(ApiException.class,
            () -> orderItemApi.getSalesByDate(null, now)
        );
        assertEquals("Start date cannot be null", startEx.getMessage());

        ApiException endEx = assertThrows(ApiException.class,
            () -> orderItemApi.getSalesByDate(now, null)
        );
        assertEquals("End date cannot be null", endEx.getMessage());
    }

    @Test
    public void getSalesByDate_invalidRange_throwsException() {
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.minusDays(1);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.getSalesByDate(start, end)
        );
        assertEquals("Start date cannot be after end date.", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // update()
    // ---------------------------------------------------------------------

    @Test
    public void update_existingItem_updatesFields() throws ApiException {
        OrderItem existing = mockPersistedObject(1, 1, 1);
        when(orderItemDao.selectById(1)).thenReturn(existing);

        OrderItem updates = mockPersistedObject(1, 1, 1);
        updates.setQuantity(20);
        updates.setSellingPrice(150.0);

        OrderItem result = orderItemApi.update(updates);

        assertSame(existing, result);
        assertEquals(Integer.valueOf(20), existing.getQuantity());
        assertEquals(150.0, existing.getSellingPrice(), 0.001);
    }

    @Test
    public void update_nullItem_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.update(null)
        );
        assertEquals("Order item object cannot be null", ex.getMessage());
    }

    @Test
    public void update_missingItem_throwsException() {
        when(orderItemDao.selectById(5)).thenReturn(null);

        OrderItem updates = mockPersistedObject(5, 1, 1);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.update(updates)
        );
        assertEquals("Order item doesn't exist", ex.getMessage());
    }

    @Test
    public void update_orderIdMismatch_throwsException() {
        OrderItem existing = mockPersistedObject(7, 10, 1);
        when(orderItemDao.selectById(7)).thenReturn(existing);

        OrderItem updates = mockPersistedObject(7, 11, 1);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.update(updates)
        );
        assertEquals("Order 11 doesn't have order item 7", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // deleteById(Integer)
    // ---------------------------------------------------------------------

    @Test
    public void deleteById_singleArg_existingItem_deletes() throws ApiException {
        OrderItem existing = mockPersistedObject(9, 1, 1);
        when(orderItemDao.selectById(9)).thenReturn(existing);

        orderItemApi.deleteById(9);

        verify(orderItemDao).deleteById(9);
    }

    @Test
    public void deleteById_singleArg_nullId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.deleteById((Integer) null)
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    @Test
    public void deleteById_singleArg_missingItem_throwsException() {
        when(orderItemDao.selectById(15)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.deleteById(15)
        );
        assertEquals("Order Item doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // deleteById(Integer, Integer)
    // ---------------------------------------------------------------------

    @Test
    public void deleteById_doubleArg_existingItem_deletes() throws ApiException {
        OrderItem existing = mockPersistedObject(20, 5, 1);
        when(orderItemDao.selectById(20)).thenReturn(existing);

        orderItemApi.deleteById(20, 5);

        verify(orderItemDao).deleteById(20);
    }

    @Test
    public void deleteById_doubleArg_nullIds_throwExceptions() {
        ApiException itemEx = assertThrows(ApiException.class,
            () -> orderItemApi.deleteById(null, 5)
        );
        assertEquals("Item id cannot be null", itemEx.getMessage());

        ApiException orderEx = assertThrows(ApiException.class,
            () -> orderItemApi.deleteById(5, null)
        );
        assertEquals("Order Id cannot be null", orderEx.getMessage());
    }

    @Test
    public void deleteById_doubleArg_missingItem_throwsException() {
        when(orderItemDao.selectById(30)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.deleteById(30, 3)
        );
        assertEquals("Order item doesn't exist", ex.getMessage());
    }

    @Test
    public void deleteById_doubleArg_orderMismatch_throwsException() {
        OrderItem existing = mockPersistedObject(25, 2, 1);
        when(orderItemDao.selectById(25)).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class,
            () -> orderItemApi.deleteById(25, 10)
        );
        assertEquals("Order 10 doesn't have the item 25", ex.getMessage());
    }
}
