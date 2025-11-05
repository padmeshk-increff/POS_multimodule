package com.increff.pos.unit.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.result.ConversionResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.Collections;

import static com.increff.pos.factory.ProductFactory.mockNewObject;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Behavior-focused unit tests for ProductFlow.
 * Tests the CONTRACT (input → output) not implementation details.
 */
public class ProductFlowTest {

    @Mock
    private ProductApi productApi;
    @Mock
    private ClientApi clientApi;
    @Mock
    private InventoryApi inventoryApi;

    @InjectMocks
    private ProductFlow productFlow;

    private Client mockClient;
    private ConversionResult<String[]> tsvResult;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        mockClient = new Client();
        mockClient.setId(1);
        mockClient.setClientName("test-client");
        
        // Setup TSV test data
        String[] tsvRow = {"BC123", "Product A", "100.0", "test-client", "category-a"};
        tsvResult = new ConversionResult<>();
        tsvResult.setValidRows(Collections.singletonList(tsvRow));
        tsvResult.setErrors(Collections.emptyList());
    }

    // --- insert() Tests ---

    @Test
    public void insertValidProductShouldReturnPersistedProductWithInventory() throws ApiException {
        // GIVEN
        Product newProduct = mockNewObject("BC123", 1);
        when(clientApi.getCheckById(1)).thenReturn(mockClient);
        when(productApi.insert(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(100);
            return p;
        });
        when(inventoryApi.insert(any())).thenReturn(null);

        // WHEN
        Product result = productFlow.insert(newProduct);

        // THEN - Test BEHAVIOR: product persisted with ID
        assertNotNull(result.getId());
        assertEquals("BC123", result.getBarcode());
        
        // Verify critical side effect: inventory created with quantity 0
        verify(inventoryApi).insert(argThat(inv ->
            inv.getProductId().equals(result.getId()) && inv.getQuantity() == 0
        ));
    }

    @Test
    public void insertInvalidClientIdShouldThrowException() throws ApiException {
        // GIVEN
        Product newProduct = mockNewObject("BC123", 999);
        when(clientApi.getCheckById(999)).thenThrow(new ApiException("Client not found"));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> productFlow.insert(newProduct)
        );
        assertEquals("Client not found", ex.getMessage());
    }

    @Test
    public void insertDuplicateBarcodeShouldThrowException() throws ApiException {
        // GIVEN
        Product newProduct = mockNewObject("DUPLICATE", 1);
        when(clientApi.getCheckById(1)).thenReturn(mockClient);
        when(productApi.insert(any())).thenThrow(new ApiException("Duplicate barcode"));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> productFlow.insert(newProduct)
        );
        assertEquals("Duplicate barcode", ex.getMessage());
    }

    // --- deleteById() Tests ---

    @Test
    public void deleteByIdValidIdShouldSucceed() throws ApiException {
        // GIVEN
        doNothing().when(productApi).deleteById(1);
        doNothing().when(inventoryApi).deleteById(1);

        // WHEN
        productFlow.deleteById(1);

        // THEN - Only verify critical side effects
        verify(inventoryApi).deleteById(1);
    }

    @Test
    public void deleteByIdProductNotFoundShouldThrowException() throws ApiException {
        // GIVEN
        doThrow(new ApiException("Product not found")).when(productApi).deleteById(999);

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> productFlow.deleteById(999)
        );
        assertEquals("Product not found", ex.getMessage());
    }

    // NOTE: uploadByFile() is complex with many util calls.
    // It's better tested via integration tests.
    // Here we only test critical error handling.
    
    @Test
    public void uploadByFileNullInputShouldThrowException() {
        // WHEN/THEN
        assertThrows(NullPointerException.class,
            () -> productFlow.uploadByFile(null)
        );
    }
}