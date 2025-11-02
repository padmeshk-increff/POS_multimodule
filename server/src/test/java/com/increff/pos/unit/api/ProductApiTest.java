package com.increff.pos.unit.api;

import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.ProductDao;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.ProductUploadRow;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.model.result.ProductUploadResult;
import com.increff.pos.utils.BaseUtil;
import com.increff.pos.utils.ProductUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.increff.pos.factory.ProductFactory.mockNewObject;
import static com.increff.pos.factory.ProductFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ProductApi class.
 * Mocks the ProductDao to test all validation and business logic.
 */
public class ProductApiTest {

    @Mock
    private ProductDao productDao;

    @Mock
    private Pageable mockPageable;

    @InjectMocks
    private ProductApi productApi;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- insert() Tests ---

    @Test
    public void insert_validProduct_shouldSucceed() throws ApiException {
        // Given
        Product newProduct = mockNewObject("barcode1", 1);
        when(productDao.selectByBarcode("barcode1")).thenReturn(null);

        // When
        Product savedProduct = productApi.insert(newProduct);

        // Then
        assertNotNull(savedProduct);
        verify(productDao, times(1)).selectByBarcode("barcode1");
        verify(productDao, times(1)).insert(newProduct);
    }

    @Test
    public void insert_nullProduct_shouldThrowApiException() {
        try {
            productApi.insert(null);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Product object cannot be null", e.getMessage());
        }
    }

    @Test
    public void insert_duplicateBarcode_shouldThrowApiException() {
        // Given
        Product newProduct = mockNewObject("barcode1", 1);
        Product existing = mockPersistedObject("barcode1");
        when(productDao.selectByBarcode("barcode1")).thenReturn(existing);

        // When/Then
        try {
            productApi.insert(newProduct);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Product already exists", e.getMessage());
        }
    }

     //--- getFilteredProducts() Tests ---

    @Test
    public void getFilteredProducts_withResults_shouldReturnPaginatedResult() throws ApiException {
        // Given
        when(productDao.countWithFilters(any(), any(), any(), any(), any())).thenReturn(10L);
        when(productDao.selectWithFilters(any(), any(), any(), any(), any(), eq(mockPageable)))
                .thenReturn(Arrays.asList(mockPersistedObject(1), mockPersistedObject(2)));
        when(mockPageable.getPageSize()).thenReturn(5);

        // When
        PaginatedResult<Product> result = productApi.getFilteredProducts(null, null, null, null, null, mockPageable);

        // Then
        assertEquals(10L, (long) result.getTotalElements());
        assertEquals(2, (int) result.getTotalPages());
        assertEquals(2, result.getResults().size());
    }

    @Test
    public void getFilteredProducts_noResults_shouldReturnEmptyResult() throws ApiException {
        // --- GIVEN ---
        when(productDao.countWithFilters(any(), any(), any(), any(), any())).thenReturn(0L);

        // Prepare the empty result
        PaginatedResult<Product> emptyResult = new PaginatedResult<>();
        emptyResult.setResults(Collections.emptyList());
        emptyResult.setTotalElements(0L);
        emptyResult.setTotalPages(0);

        // Mock both ProductUtil and BaseUtil to be safe (createEmptyResult is in BaseUtil)
        try (
                MockedStatic<BaseUtil> mockedBase = Mockito.mockStatic(BaseUtil.class)
        ) {
            mockedBase.when(BaseUtil::createEmptyResult).thenReturn(emptyResult);

            // --- WHEN ---
            PaginatedResult<Product> result = productApi.getFilteredProducts(null, null, null, null, null, mockPageable);

            // --- THEN (Asserts) ---
            assertNotNull(result);
            assertEquals(0L, (long) result.getTotalElements());
            assertTrue(result.getResults().isEmpty());
        }

        // --- THEN (Verify) ---
        verify(productDao, times(1)).countWithFilters(any(), any(), any(), any(), any());
        verify(productDao, never()).selectWithFilters(any(), any(), any(), any(), any(), any());
    }


    @Test
    public void getFilteredProducts_nullPageable_shouldThrowApiException() {
        try {
            productApi.getFilteredProducts(null, null, null, null, null, null);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Pageable object cannot be null", e.getMessage());
        }
    }

    // --- getById() Tests ---

    @Test
    public void getById_existing_shouldReturnProduct() throws ApiException {
        // Given
        Product product = mockPersistedObject(1);
        when(productDao.selectById(1)).thenReturn(product);

        // When
        Product result = productApi.getById(1);

        // Then
        assertNotNull(result);
        assertEquals(product, result);
    }

    @Test
    public void getById_nonExisting_shouldReturnNull() throws ApiException {
        // Given
        when(productDao.selectById(999)).thenReturn(null);

        // When
        Product result = productApi.getById(999);

        // Then
        assertNull(result);
    }

    @Test
    public void getById_nullId_shouldThrowApiException() {
        try {
            productApi.getById(null);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Id cannot be null", e.getMessage());
        }
    }

    // --- getByIds() Tests ---

    @Test
    public void getByIds_valid_shouldReturnList() throws ApiException {
        // Given
        List<Integer> ids = Arrays.asList(1, 2);
        when(productDao.selectByIds(ids)).thenReturn(Arrays.asList(mockPersistedObject(1), mockPersistedObject(2)));

        // When
        List<Product> result = productApi.getByIds(ids);

        // Then
        assertEquals(2, result.size());
    }

    @Test
    public void getByIds_null_shouldThrowApiException() {
        try {
            productApi.getByIds(null);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Ids cannot be null", e.getMessage());
        }
    }

    // --- getCheckById() Tests ---

    @Test
    public void getCheckById_nonExisting_shouldThrowApiException() {
        // Given
        when(productDao.selectById(999)).thenReturn(null);

        // When/Then
        try {
            productApi.getCheckById(999);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Product 999 doesn't exist", e.getMessage());
        }
    }

    // --- getCheckByBarcode() Tests ---

    @Test
    public void getCheckByBarcode_nonExisting_shouldThrowApiException() {
        // Given
        when(productDao.selectByBarcode("fake")).thenReturn(null);

        // When/Then
        try {
            productApi.getCheckByBarcode("fake");
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Product with barcode fake doesn't exist", e.getMessage());
        }
    }

    @Test
    public void getCheckByBarcode_null_shouldThrowApiException() {
        try {
            productApi.getCheckByBarcode(null);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Barcode cannot be null", e.getMessage());
        }
    }

    // --- getByBarcode() Tests ---

    @Test
    public void getByBarcode_nonExisting_shouldReturnNull() throws ApiException {
        // Given
        when(productDao.selectByBarcode("fake")).thenReturn(null);

        // When
        Product result = productApi.getByBarcode("fake");

        // Then
        assertNull(result);
    }

    // --- updateById() Tests ---

    @Test
    public void updateById_validChange_shouldSucceed() throws ApiException {
        // Given
        Integer id = 1;
        Integer clientId = 10;
        Product existing = mockPersistedObject(id, "old-bc", clientId);
        Product updateData = mockNewObject("new-bc", clientId); // new barcode, same client
        updateData.setName("New Name");

        when(productDao.selectById(id)).thenReturn(existing);
        when(productDao.selectByBarcode("new-bc")).thenReturn(null); // No duplicate

        // When
        Product result = productApi.updateById(id, updateData);

        // Then
        verify(productDao, times(1)).update(existing);
        assertEquals("new-bc", result.getBarcode());
        assertEquals("New Name", result.getName());
    }

    @Test
    public void updateById_sameBarcode_shouldSucceed() throws ApiException {
        // Given
        Integer id = 1;
        Integer clientId = 10;
        Product existing = mockPersistedObject(id, "same-bc", clientId);
        Product updateData = mockNewObject("same-bc", clientId); // same barcode
        updateData.setName("New Name");

        when(productDao.selectById(id)).thenReturn(existing);

        // When
        Product result = productApi.updateById(id, updateData);

        // Then
        // The check for duplicate barcode should be skipped
        verify(productDao, never()).selectByBarcode(anyString());
        verify(productDao, times(1)).update(existing);
        assertEquals("New Name", result.getName());
    }

    @Test
    public void updateById_clientIdChange_shouldThrowApiException() {
        // Given
        Integer id = 1;
        Product existing = mockPersistedObject(id, "bc", 10); // Client 10
        Product updateData = mockNewObject("bc", 11); // Client 11

        when(productDao.selectById(id)).thenReturn(existing);

        // When/Then
        try {
            productApi.updateById(id, updateData);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Client id of a product can't be changed", e.getMessage());
        }
    }

    @Test
    public void updateById_duplicateBarcode_shouldThrowApiException() {
        // Given
        Integer id = 1;
        Product existing = mockPersistedObject(id, "old-bc", 10);
        Product updateData = mockNewObject("new-bc", 10); // trying to change to new-bc
        Product duplicate = mockPersistedObject(2, "new-bc", 10); // another product has new-bc

        when(productDao.selectById(id)).thenReturn(existing);
        when(productDao.selectByBarcode("new-bc")).thenReturn(duplicate); // duplicate found

        // When/Then
        try {
            productApi.updateById(id, updateData);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Another product with the barcode 'new-bc' already exists.", e.getMessage());
        }
    }

    @Test
    public void updateById_productNotFound_shouldThrowApiException() {
        // Given
        when(productDao.selectById(999)).thenReturn(null);

        // When/Then
        try {
            productApi.updateById(999, mockNewObject("bc", 1));
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Product doesn't exist", e.getMessage());
        }
    }

    // --- upload() Tests (Requires mockito-inline) ---

    @Test
    public void upload_withSuccessAndFailures_shouldReturnResult() throws ApiException {
        // 1. Given: Setup rows
        ProductUploadRow rowSuccess = new ProductUploadRow();
        rowSuccess.setBarcode("bc1");
        ProductUploadRow rowFailValidate = new ProductUploadRow();
        rowFailValidate.setBarcode("bc2");
        ProductUploadRow rowFailFileDupe = new ProductUploadRow();
        rowFailFileDupe.setBarcode("file-dupe");
        ProductUploadRow rowFailFileDupe2 = new ProductUploadRow();
        rowFailFileDupe2.setBarcode(" file-dupe "); // Test normalization

        List<ProductUploadRow> candidateRows = Arrays.asList(rowSuccess, rowFailValidate, rowFailFileDupe, rowFailFileDupe2);

        // 2. Given: Setup maps
        Map<String, Client> clientMap = new HashMap<>();
        Set<String> existingBarcodesInDb = new HashSet<>();

        // 3. Mock static ProductUtil
        try (MockedStatic<ProductUtil> mockedUtil = Mockito.mockStatic(ProductUtil.class)) {

            // Mock 1: File duplicate check
            // We return the normalized barcode "file-dupe"
            mockedUtil.when(() -> ProductUtil.findDuplicateBarcodesInFile(candidateRows))
                    .thenReturn(new HashSet<>(Collections.singletonList("file-dupe")));

            // Mock 2: Success row
            Product productSuccess = mockNewObject("bc1", 1);
            mockedUtil.when(() -> ProductUtil.validateAndConvert(eq(rowSuccess), anyMap(), anySet()))
                    .thenReturn(productSuccess);

            // Mock 3: Failed validation row
            ApiException validationError = new ApiException("Invalid MRP");
            mockedUtil.when(() -> ProductUtil.validateAndConvert(eq(rowFailValidate), anyMap(), anySet()))
                    .thenThrow(validationError);

            // 4. When
            ProductUploadResult result = productApi.upload(candidateRows, clientMap, existingBarcodesInDb);

            // 5. Then
            // Assert successful part
            assertEquals(1, result.getSuccessfullyInserted().size());
            assertEquals(productSuccess, result.getSuccessfullyInserted().get(0));

            // Assert failed part (3 failures)
            assertEquals(3, result.getFailedRows().size());
            // Check validation failure
            assertEquals(rowFailValidate, result.getFailedRows().get(0).getRow());
            assertEquals("Invalid MRP", result.getFailedRows().get(0).getErrorMessage());
            // Check file duplicate failures
            assertEquals(rowFailFileDupe, result.getFailedRows().get(1).getRow());
            assertTrue(result.getFailedRows().get(1).getErrorMessage().contains("Duplicate barcode"));
            assertEquals(rowFailFileDupe2, result.getFailedRows().get(2).getRow());
            assertTrue(result.getFailedRows().get(2).getErrorMessage().contains("Duplicate barcode"));

            // Verify bulk insert was called ONLY with the successful item
            verify(productDao, times(1)).insertAll(argThat(list ->
                    list.size() == 1 && list.contains(productSuccess)
            ));
        }
    }

    // --- deleteById() Tests ---

    @Test
    public void deleteById_existing_shouldSucceed() throws ApiException {
        // Given
        when(productDao.selectById(1)).thenReturn(mockPersistedObject(1));

        // When
        productApi.deleteById(1);

        // Then
        verify(productDao, times(1)).selectById(1);
        verify(productDao, times(1)).deleteById(1);
    }

    @Test
    public void deleteById_notFound_shouldThrowApiException() {
        // Given
        when(productDao.selectById(999)).thenReturn(null);

        // When/Then
        try {
            productApi.deleteById(999);
            fail("Should have thrown");
        } catch (ApiException e) {
            assertEquals("Product 999 doesn't exist", e.getMessage());
        }
    }
}