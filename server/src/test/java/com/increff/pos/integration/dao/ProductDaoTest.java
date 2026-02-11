package com.increff.pos.integration.dao;

import com.increff.pos.config.TestDbConfig;
import com.increff.pos.dao.ClientDao;
import com.increff.pos.dao.ProductDao;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Product;
import com.increff.pos.factory.ClientFactory;
import com.increff.pos.factory.ProductFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestDbConfig.class)
@TestPropertySource("classpath:test.properties")
@Transactional
public class ProductDaoTest {

    @Autowired
    private ProductDao productDao;
    
    @Autowired
    private ClientDao clientDao;

    private Client testClient;

    @Before
    public void setUp() {
        // Create prerequisite client
        testClient = ClientFactory.mockNewObject("test-client-" + System.currentTimeMillis());
        clientDao.insert(testClient);
    }

    // --- Tests for AbstractDao methods ---

    @Test
    public void testInsertAndSelectById() {
        // Arrange
        Product p = ProductFactory.mockNewObject("TEST-BARCODE", testClient.getId());
        
        // Act
        productDao.insert(p);
        
        // Assert
        assertNotNull(p.getId());
        Product fromDb = productDao.selectById(p.getId());
        assertNotNull(fromDb);
        assertEquals("TEST-BARCODE", fromDb.getBarcode());
        assertEquals(testClient.getId(), fromDb.getClientId());
    }

    @Test
    public void testSelectAll() {
        // Arrange
        productDao.insert(ProductFactory.mockNewObject("BARCODE-1", testClient.getId()));
        productDao.insert(ProductFactory.mockNewObject("BARCODE-2", testClient.getId()));
        
        // Act
        List<Product> all = productDao.selectAll();
        
        // Assert
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdate() {
        // Arrange
        Product p = ProductFactory.mockNewObject("ORIGINAL-BC", testClient.getId());
        productDao.insert(p);
        Double originalMrp = p.getMrp();
        
        // Act
        p.setMrp(999.99);
        productDao.update(p);
        Product fromDb = productDao.selectById(p.getId());
        
        // Assert
        assertEquals(999.99, fromDb.getMrp(), 0.001);
        assertNotEquals(originalMrp, fromDb.getMrp());
    }

    @Test
    public void testDeleteById() {
        // Arrange
        Product p = ProductFactory.mockNewObject("DELETE-BC", testClient.getId());
        productDao.insert(p);
        Integer id = p.getId();
        
        // Act
        productDao.deleteById(id);
        Product fromDb = productDao.selectById(id);
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testInsertAllAndSelectByIds() {
        // Arrange
        List<Product> products = Arrays.asList(
                ProductFactory.mockNewObject("BATCH-1", testClient.getId()),
                ProductFactory.mockNewObject("BATCH-2", testClient.getId())
        );
        
        // Act
        productDao.insertAll(products);
        List<Integer> ids = Arrays.asList(products.get(0).getId(), products.get(1).getId());
        List<Product> fromDb = productDao.selectByIds(ids);
        
        // Assert
        assertEquals(2, fromDb.size());
    }

    // --- Tests for ProductDao specific methods ---

    @Test
    public void testSelectByBarcodeFound() {
        // Arrange
        String uniqueBarcode = "UNIQUE-BC-" + System.currentTimeMillis();
        Product p = ProductFactory.mockNewObject(uniqueBarcode, testClient.getId());
        productDao.insert(p);
        
        // Act
        Product fromDb = productDao.selectByBarcode(uniqueBarcode);
        
        // Assert
        assertNotNull(fromDb);
        assertEquals(p.getId(), fromDb.getId());
    }

    @Test
    public void testSelectByBarcodeNotFound() {
        // Act
        Product fromDb = productDao.selectByBarcode("NON-EXISTENT-BC");
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testSelectByBarcodes() {
        // Arrange
        String bc1 = "BC1-" + System.currentTimeMillis();
        String bc2 = "BC2-" + System.currentTimeMillis();
        productDao.insert(ProductFactory.mockNewObject(bc1, testClient.getId()));
        productDao.insert(ProductFactory.mockNewObject(bc2, testClient.getId()));
        
        // Act
        List<Product> fromDb = productDao.selectByBarcodes(Arrays.asList(bc1, bc2));
        
        // Assert
        assertEquals(2, fromDb.size());
    }

    // --- Tests for Filter methods ---

    @Test
    public void testFiltersNoFilter() {
        // Arrange
        productDao.insert(ProductFactory.mockNewObject("FILTER-1", testClient.getId()));
        productDao.insert(ProductFactory.mockNewObject("FILTER-2", testClient.getId()));
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        List<Product> results = productDao.selectWithFilters(null, null, null, null, null, pageable);
        Long count = productDao.countWithFilters(null, null, null, null, null);
        
        // Assert
        assertEquals(2, results.size());
        assertEquals(2, (long) count);
    }

    @Test
    public void testFiltersWithSearchTerm() {
        // Arrange
        String uniqueTerm = "SEARCH_" + System.currentTimeMillis();

        Product p = ProductFactory.mockNewObject(uniqueTerm + "_BC", testClient.getId());
        productDao.insert(p);
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        // Search uses prefix matching on name and barcode, so uniqueTerm should match barcode starting with uniqueTerm
        List<Product> results = productDao.selectWithFilters(uniqueTerm, null, null, null, null, pageable);
        Long count = productDao.countWithFilters(uniqueTerm, null, null, null, null);
        
        // Assert
        assertEquals(1, (long) count);
        // With prefix matching, barcode should start with the search term (case-insensitive)
        assertTrue(results.stream().anyMatch(prod -> 
            prod.getBarcode().toLowerCase().startsWith(uniqueTerm.toLowerCase())
        ));
    }

    @Test
    public void testFiltersWithClientName() {
        // Arrange
        String uniqueClientName = "FilterClient_" + System.currentTimeMillis();
        Client filterClient = ClientFactory.mockNewObject(uniqueClientName);
        clientDao.insert(filterClient);
        
        productDao.insert(ProductFactory.mockNewObject("BC-CLIENT", filterClient.getId()));
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        List<Product> results = productDao.selectWithFilters(null, uniqueClientName, null, null, null, pageable);
        Long count = productDao.countWithFilters(null, uniqueClientName, null, null, null);
        
        // Assert
        assertEquals(1, (long) count);
        assertEquals(1, results.size());
    }

    @Test
    public void testFiltersWithMrpRange() {
        // Arrange
        Product p1 = ProductFactory.mockNewObject("MRP-150", testClient.getId());
        p1.setMrp(150.0);
        productDao.insert(p1);
        
        Product p2 = ProductFactory.mockNewObject("MRP-50", testClient.getId());
        p2.setMrp(50.0);
        productDao.insert(p2);
        
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        List<Product> results = productDao.selectWithFilters(null, null, null, 100.0, 200.0, pageable);
        Long count = productDao.countWithFilters(null, null, null, 100.0, 200.0);
        
        // Assert
        assertEquals(1, (long) count);
        assertTrue(results.stream().anyMatch(p -> p.getMrp() == 150.0));
        assertFalse(results.stream().anyMatch(p -> p.getMrp() == 50.0));
    }

    @Test
    public void testFiltersPaginationAndSorting() {
        // Arrange
        String uniquePrefix = "SORT_" + System.currentTimeMillis();

        Product p1 = ProductFactory.mockNewObject(uniquePrefix + "_CCC", testClient.getId());
        Product p2 = ProductFactory.mockNewObject(uniquePrefix + "_AAA", testClient.getId());
        Product p3 = ProductFactory.mockNewObject(uniquePrefix + "_BBB", testClient.getId());
        productDao.insert(p1);
        productDao.insert(p2);
        productDao.insert(p3);
        
        Pageable pageable = PageRequest.of(0, 2, Sort.by("barcode").ascending());
        
        // Act
        List<Product> results = productDao.selectWithFilters(uniquePrefix, null, null, null, null, pageable);
        Long count = productDao.countWithFilters(uniquePrefix, null, null, null, null);
        
        // Assert
        assertEquals(3, (long) count);
        assertEquals(2, results.size()); // Page size
        assertTrue(results.get(0).getBarcode().contains("_AAA"));
        assertTrue(results.get(1).getBarcode().contains("_BBB"));
    }
}
