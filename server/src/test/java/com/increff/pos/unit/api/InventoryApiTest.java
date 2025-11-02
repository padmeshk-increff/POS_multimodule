package com.increff.pos.unit.api;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.InventoryDao;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.InventoryUploadRow;
import com.increff.pos.model.result.InventoryReportResult;
import com.increff.pos.model.result.InventoryUploadResult;
import com.increff.pos.utils.InventoryUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.increff.pos.factory.InventoryFactory.mockNewObject;
import static com.increff.pos.factory.InventoryFactory.mockPersistedObject;
import static com.increff.pos.factory.InventoryFactory.mockPersistedObjectWithId;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behavior-focused unit tests for {@link InventoryApi}.
 * These tests concentrate on the observable contract of each public method.
 */
public class InventoryApiTest {

    @Mock
    private InventoryDao inventoryDao;

    @InjectMocks
    private InventoryApi inventoryApi;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------------------------------------------------------
    // insert()
    // ---------------------------------------------------------------------

    @Test
    public void insert_validInventory_returnsSameInstance() throws ApiException {
        Inventory newInventory = mockNewObject(1);
        when(inventoryDao.selectById(1)).thenReturn(null);

        Inventory saved = inventoryApi.insert(newInventory);

        assertSame(newInventory, saved);
    }

    @Test
    public void insert_nullInventory_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.insert(null)
        );
        assertEquals("Inventory cannot be null", ex.getMessage());
    }

    @Test
    public void insert_existingInventory_throwsException() {
        Inventory newInventory = mockNewObject(10);
        when(inventoryDao.selectById(10)).thenReturn(mockPersistedObjectWithId(10));

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.insert(newInventory)
        );
        assertEquals("inventory already exists", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getCheckByProductId()
    // ---------------------------------------------------------------------

    @Test
    public void getCheckByProductId_existingProduct_returnsInventory() throws ApiException {
        Inventory existing = mockPersistedObject(5, 42);
        when(inventoryDao.selectByProductId(5)).thenReturn(existing);

        Inventory result = inventoryApi.getCheckByProductId(5);

        assertSame(existing, result);
    }

    @Test
    public void getCheckByProductId_nullId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.getCheckByProductId(null)
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    @Test
    public void getCheckByProductId_missingProduct_throwsException() {
        when(inventoryDao.selectByProductId(999)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.getCheckByProductId(999)
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getCheckById()
    // ---------------------------------------------------------------------

    @Test
    public void getCheckById_existingId_returnsInventory() throws ApiException {
        Inventory existing = mockPersistedObjectWithId(7);
        when(inventoryDao.selectById(7)).thenReturn(existing);

        Inventory result = inventoryApi.getCheckById(7);

        assertSame(existing, result);
    }

    @Test
    public void getCheckById_nullId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.getCheckById(null)
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    @Test
    public void getCheckById_missingId_throwsException() {
        when(inventoryDao.selectById(404)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.getCheckById(404)
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getByProductIds() & getAll()
    // ---------------------------------------------------------------------

    @Test
    public void getByProductIds_returnsMatchingRecords() {
        List<Integer> ids = Arrays.asList(1, 2);
        List<Inventory> expected = Arrays.asList(mockPersistedObject(1, 10), mockPersistedObject(2, 20));
        when(inventoryDao.selectByProductIds(ids)).thenReturn(expected);

        List<Inventory> result = inventoryApi.getByProductIds(ids);

        assertEquals(expected, result);
    }

    @Test
    public void getAll_returnsAllInventory() {
        List<Inventory> expected = Arrays.asList(mockPersistedObject(), mockPersistedObject());
        when(inventoryDao.selectAll()).thenReturn(expected);

        List<Inventory> result = inventoryApi.getAll();

        assertEquals(expected, result);
    }

    // ---------------------------------------------------------------------
    // initializeInventory()
    // ---------------------------------------------------------------------

    @Test
    public void initializeInventory_convertsProductsToZeroQuantityInventories() throws ApiException {
        Product p1 = new Product(); p1.setId(1);
        Product p2 = new Product(); p2.setId(2);
        ArgumentCaptor<List<Inventory>> captor = ArgumentCaptor.forClass(List.class);

        inventoryApi.initializeInventory(Arrays.asList(p1, p2));

        verify(inventoryDao).insertAll(captor.capture());
        List<Inventory> captured = captor.getValue();
        assertEquals(2, captured.size());
        assertEquals(Integer.valueOf(1), captured.get(0).getProductId());
        assertEquals(Integer.valueOf(0), captured.get(0).getQuantity());
        assertEquals(Integer.valueOf(2), captured.get(1).getProductId());
        assertEquals(Integer.valueOf(0), captured.get(1).getQuantity());
    }

    @Test
    public void initializeInventory_emptyInput_doesNothing() throws ApiException {
        inventoryApi.initializeInventory(Collections.emptyList());

        verify(inventoryDao, never()).insertAll(anyList());
    }

    // ---------------------------------------------------------------------
    // getLowStockItems() & getInventoryReportData()
    // ---------------------------------------------------------------------

    @Test
    public void getLowStockItems_validThreshold_returnsList() throws ApiException {
        List<Inventory> expected = Collections.singletonList(mockPersistedObject(10, 3));
        when(inventoryDao.selectLowStockItems(5)).thenReturn(expected);

        List<Inventory> result = inventoryApi.getLowStockItems(5);

        assertEquals(expected, result);
    }

    @Test
    public void getLowStockItems_nullThreshold_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.getLowStockItems(null)
        );
        assertEquals("Threshold cannot be null", ex.getMessage());
    }

    @Test
    public void getInventoryReportData_delegatesToDao() {
        InventoryReportResult resultRow = new InventoryReportResult(
            1,
            "Test Product",
            "BARCODE",
            "Category",
            99.99,
            42
        );
        List<InventoryReportResult> expected = Collections.singletonList(resultRow);
        when(inventoryDao.findInventoryReportData()).thenReturn(expected);

        List<InventoryReportResult> result = inventoryApi.getInventoryReportData();

        assertEquals(expected, result);
    }

    // ---------------------------------------------------------------------
    // update() (by product id)
    // ---------------------------------------------------------------------

    @Test
    public void update_existingInventory_updatesQuantity() throws ApiException {
        Inventory updates = mockNewObject(1);
        updates.setQuantity(50);
        Inventory existing = mockPersistedObject(1, 10);
        when(inventoryDao.selectByProductId(1)).thenReturn(existing);

        inventoryApi.update(updates);

        assertEquals(Integer.valueOf(50), existing.getQuantity());
    }

    @Test
    public void update_nullInventory_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.update(null)
        );
        assertEquals("Inventory cannot be null", ex.getMessage());
    }

    @Test
    public void update_missingInventory_throwsException() {
        Inventory updates = mockNewObject(999);
        when(inventoryDao.selectByProductId(999)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.update(updates)
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // updateById()
    // ---------------------------------------------------------------------

    @Test
    public void updateById_existingId_updatesQuantity() throws ApiException {
        Inventory updates = mockNewObject(999);
        updates.setQuantity(75);
        Inventory existing = mockPersistedObjectWithId(1);
        existing.setQuantity(10);
        when(inventoryDao.selectById(1)).thenReturn(existing);

        Inventory result = inventoryApi.updateById(1, updates);

        assertSame(existing, result);
        assertEquals(Integer.valueOf(75), existing.getQuantity());
    }

    @Test
    public void updateById_nullId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.updateById(null, mockNewObject(1))
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    @Test
    public void updateById_nullInventory_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.updateById(1, null)
        );
        assertEquals("Inventory cannot be null", ex.getMessage());
    }

    @Test
    public void updateById_missingInventory_throwsException() {
        when(inventoryDao.selectById(5)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.updateById(5, mockNewObject(5))
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // deleteById()
    // ---------------------------------------------------------------------

    @Test
    public void deleteById_existingInventory_succeeds() throws ApiException {
        when(inventoryDao.selectById(3)).thenReturn(mockPersistedObjectWithId(3));

        inventoryApi.deleteById(3);

        verify(inventoryDao).deleteById(3);
    }

    @Test
    public void deleteById_nullId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.deleteById(null)
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    @Test
    public void deleteById_missingInventory_throwsException() {
        when(inventoryDao.selectById(9)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.deleteById(9)
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // updateQuantityByProductId()
    // ---------------------------------------------------------------------

    @Test
    public void updateQuantityByProductId_decreaseStock_updatesQuantity() throws ApiException {
        Inventory existing = mockPersistedObject(1, 100);
        when(inventoryDao.selectByProductId(1)).thenReturn(existing);

        inventoryApi.updateQuantityByProductId(1, 10, 25);

        assertEquals(Integer.valueOf(85), existing.getQuantity());
    }

    @Test
    public void updateQuantityByProductId_notEnoughStock_throwsException() {
        Inventory existing = mockPersistedObject(1, 5);
        when(inventoryDao.selectByProductId(1)).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.updateQuantityByProductId(1, 1, 10)
        );
        assertEquals("Not enough items in stock", ex.getMessage());
        assertEquals(Integer.valueOf(5), existing.getQuantity());
    }

    @Test
    public void updateQuantityByProductId_nullArguments_throwExceptions() {
        ApiException productEx = assertThrows(ApiException.class,
            () -> inventoryApi.updateQuantityByProductId(null, 1, 1)
        );
        assertEquals("Product Id cannot be null", productEx.getMessage());

        ApiException oldQtyEx = assertThrows(ApiException.class,
            () -> inventoryApi.updateQuantityByProductId(1, null, 1)
        );
        assertEquals("Old quantity cannot be null", oldQtyEx.getMessage());

        ApiException newQtyEx = assertThrows(ApiException.class,
            () -> inventoryApi.updateQuantityByProductId(1, 1, null)
        );
        assertEquals("New Quantity cannot be null", newQtyEx.getMessage());
    }

    @Test
    public void updateQuantityByProductId_missingInventory_throwsException() {
        when(inventoryDao.selectByProductId(404)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.updateQuantityByProductId(404, 1, 1)
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // updateByProductId()
    // ---------------------------------------------------------------------

    @Test
    public void updateByProductId_existingInventory_updatesQuantity() throws ApiException {
        Inventory updates = mockNewObject(7);
        updates.setQuantity(33);
        Inventory existing = mockPersistedObject(7, 2);
        when(inventoryDao.selectByProductId(7)).thenReturn(existing);

        Inventory result = inventoryApi.updateByProductId(7, updates);

        assertSame(existing, result);
        assertEquals(Integer.valueOf(33), existing.getQuantity());
    }

    @Test
    public void updateByProductId_nullProductId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.updateByProductId(null, mockNewObject(1))
        );
        assertEquals("Product id cannot be null", ex.getMessage());
    }

    @Test
    public void updateByProductId_nullInventory_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.updateByProductId(1, null)
        );
        assertEquals("Inventory pojo cannot be null", ex.getMessage());
    }

    @Test
    public void updateByProductId_missingInventory_throwsException() {
        when(inventoryDao.selectByProductId(11)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.updateByProductId(11, mockNewObject(11))
        );
        assertEquals("Inventory doesn't exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getByIds()
    // ---------------------------------------------------------------------

    @Test
    public void getByIds_returnsInventories() throws ApiException {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        List<Inventory> expected = Arrays.asList(
            mockPersistedObjectWithId(1),
            mockPersistedObjectWithId(2),
            mockPersistedObjectWithId(3)
        );
        when(inventoryDao.selectByIds(ids)).thenReturn(expected);

        List<Inventory> result = inventoryApi.getByIds(ids);

        assertEquals(expected, result);
    }

    @Test
    public void getByIds_nullList_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.getByIds(null)
        );
        assertEquals("Ids cannot be null", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // upload()
    // ---------------------------------------------------------------------

    @Test
    public void upload_mixedOutcomes_returnsExpectedResult() throws ApiException {
        InventoryUploadRow rowSuccess = new InventoryUploadRow();
        InventoryUploadRow rowValidationFail = new InventoryUploadRow();
        InventoryUploadRow rowDuplicate = new InventoryUploadRow();
        rowDuplicate.setBarcode("DUP");
        List<InventoryUploadRow> rows = Arrays.asList(rowSuccess, rowValidationFail, rowDuplicate);
        Map<String, Product> productMap = new HashMap<>();

        try (MockedStatic<InventoryUtil> mockedUtil = Mockito.mockStatic(InventoryUtil.class)) {
            Set<String> duplicates = new HashSet<>(Collections.singleton("DUP"));
            mockedUtil.when(() -> InventoryUtil.findDuplicateBarcodes(rows)).thenReturn(duplicates);

            Inventory converted = mockPersistedObject(1, 25);
            mockedUtil.when(() -> InventoryUtil.validateAndConvert(eq(rowSuccess), eq(productMap), eq(duplicates)))
                .thenReturn(converted);
            mockedUtil.when(() -> InventoryUtil.validateAndConvert(eq(rowValidationFail), eq(productMap), eq(duplicates)))
                .thenThrow(new ApiException("Invalid row"));
            mockedUtil.when(() -> InventoryUtil.validateAndConvert(eq(rowDuplicate), eq(productMap), eq(duplicates)))
                .thenThrow(new ApiException("Duplicate barcode in file"));

            InventoryUploadResult result = inventoryApi.upload(rows, productMap);

            assertEquals(Collections.singletonList(converted), result.getSuccessfullyUpdated());
            assertEquals(2, result.getFailedRows().size());
            assertEquals("Invalid row", result.getFailedRows().get(0).getErrorMessage());
            assertEquals("Duplicate barcode in file", result.getFailedRows().get(1).getErrorMessage());
        }
    }

    @Test
    public void bulkUpdateInventories_validList_forwardsToDao() throws ApiException {
        List<Inventory> inventories = Arrays.asList(
            mockPersistedObject(1, 10),
            mockPersistedObject(2, 20)
        );

        inventoryApi.bulkUpdateInventories(inventories);

        verify(inventoryDao).bulkUpdate(inventories);
    }

    @Test
    public void bulkUpdateInventories_nullList_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> inventoryApi.bulkUpdateInventories(null)
        );
        assertEquals("Inventories list cannot be null", ex.getMessage());
    }

    @Test
    public void bulkUpdateInventories_emptyList_skipsDaoCall() throws ApiException {
        inventoryApi.bulkUpdateInventories(Collections.emptyList());

        verify(inventoryDao, never()).bulkUpdate(anyList());
    }
}
