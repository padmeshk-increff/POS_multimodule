package com.increff.pos.integration.dao;

import com.increff.pos.config.TestDbConfig;
import com.increff.pos.dao.ClientDao;
import com.increff.pos.entity.Client;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional; // Import this!

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import static com.increff.pos.factory.ClientFactory.mockNewObject;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class) // Tells JUnit to use Spring's test runner
@ContextConfiguration(classes = TestDbConfig.class) // Loads your test configuration
@TestPropertySource("classpath:test.properties")
@Transactional // Automatically rolls back database changes after each test
public class ClientDaoTest {
    
    // Note: Using mockNewObject() from ClientFactory creates objects with ID=null
    // which is perfect for DAO integration tests that insert into the database

    @Autowired
    private ClientDao clientDao;

    // --- Tests for AbstractDao methods ---

    @Test
    public void testInsertAndSelectById() {
        // Arrange
        Client c = mockNewObject("test-client");

        // Act
        clientDao.insert(c);

        // Assert
        assertNotNull(c.getId()); // ID is populated by the DB
        Client fromDb = clientDao.selectById(c.getId());
        assertNotNull(fromDb);
        assertEquals("test-client", fromDb.getClientName());
    } // <-- Transaction rolls back here, client is deleted

    @Test
    public void testSelectAll() {
        // Arrange
        clientDao.insert(mockNewObject("client-1"));
        clientDao.insert(mockNewObject("client-2"));

        // Act
        List<Client> allClients = clientDao.selectAll();

        // Assert
        assertEquals(2, allClients.size());
    } // <-- Transaction rolls back here

    @Test
    public void testUpdate() {
        // Arrange
        Client c = mockNewObject("original-name");
        clientDao.insert(c);

        // Act
        c.setClientName("updated-name");
        clientDao.update(c);

        Client fromDb = clientDao.selectById(c.getId());

        // Assert
        assertNotNull(fromDb);
        assertEquals("updated-name", fromDb.getClientName());
    } // <-- Transaction rolls back here

    @Test
    public void testDeleteById() {
        // Arrange
        Client c = mockNewObject("to-be-deleted");
        clientDao.insert(c);
        Integer id = c.getId();
        assertNotNull(id);

        // Act
        clientDao.deleteById(id);
        Client fromDb = clientDao.selectById(id);

        // Assert
        assertNull(fromDb);
    } // <-- Transaction rolls back here (delete is part of the transaction)

    @Test
    public void testInsertAllAndSelectByIds() {
        // Arrange
        List<Client> clients = Arrays.asList(
                mockNewObject("batch-1"),
                mockNewObject("batch-2")
        );

        // Act
        clientDao.insertAll(clients);

        List<Integer> ids = new ArrayList<>();
        for (Client c : clients) {
            assertNotNull(c.getId());
            ids.add(c.getId());
        }

        List<Client> fromDb = clientDao.selectByIds(ids);

        // Assert
        assertEquals(2, fromDb.size());
    } // <-- Transaction rolls back here

    // --- Tests for ClientDao specific methods ---

    @Test
    public void testSelectByNameFound() {
        // Arrange
        Client c1 = mockNewObject("client-a");
        Client c2 = mockNewObject("client-b");
        clientDao.insert(c1);
        clientDao.insert(c2);

        // Act
        Client fromDb = clientDao.selectByName("client-b");

        // Assert
        assertNotNull(fromDb);
        assertEquals(c2.getId(), fromDb.getId());
    }

    @Test
    public void testSelectByNameNotFound() {
        // Act
        Client fromDb = clientDao.selectByName("non-existent");
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testSelectByNames() {
        // Arrange
        clientDao.insert(mockNewObject("client-a"));
        clientDao.insert(mockNewObject("client-b"));
        clientDao.insert(mockNewObject("client-c"));
        List<String> names = Arrays.asList("client-a", "client-c");

        // Act
        List<Client> fromDb = clientDao.selectByNames(names);

        // Assert
        assertEquals(2, fromDb.size());
    }

    // --- Tests for Filter methods ---

    @Test
    public void testFiltersNoFilter() {
        // Arrange
        clientDao.insert(mockNewObject("apple"));
        clientDao.insert(mockNewObject("banana"));
        Pageable pageable = PageRequest.of(0, 100); // Increase page size to get all records

        // Act
        List<Client> results = clientDao.selectWithFilters(null, pageable);
        Long count = clientDao.countWithFilters(null);

        // Assert
        assertTrue(results.size() >= 2); // At least our 2 records
        assertEquals(2, (long) count);
    }

    @Test
    public void testFiltersWithNameFilter() {
        // Arrange
        // Use unique prefix to avoid conflicts with existing data
        String uniquePrefix = "test_filter_" + System.currentTimeMillis();
        
        clientDao.insert(mockNewObject(uniquePrefix + "_apple_inc"));
        clientDao.insert(mockNewObject(uniquePrefix + "_banana_inc"));
        clientDao.insert(mockNewObject("orange"));
        Pageable pageable = PageRequest.of(0, 100);

        // Act
        List<Client> results = clientDao.selectWithFilters(uniquePrefix, pageable);
        Long count = clientDao.countWithFilters(uniquePrefix);

        // Assert
        assertEquals(2, (long) count);
        assertEquals(2, results.size());
    }

    @Test
    public void testFiltersPaginationAndSorting() {
        // Arrange
        // Use unique prefix to test sorting in isolation
        String uniquePrefix = "ztest_sort_" + System.currentTimeMillis();
        
        clientDao.insert(mockNewObject(uniquePrefix + "_ccc"));
        clientDao.insert(mockNewObject(uniquePrefix + "_aaa"));
        clientDao.insert(mockNewObject(uniquePrefix + "_bbb"));

        Pageable pageable = PageRequest.of(0, 2, Sort.by("clientName").ascending());

        // Act
        List<Client> results = clientDao.selectWithFilters(uniquePrefix, pageable);
        Long count = clientDao.countWithFilters(uniquePrefix);

        // Assert
        assertEquals(3, (long) count); // Total count includes our 3
        assertEquals(2, results.size()); // Page size is 2
        assertTrue(results.get(0).getClientName().contains("_aaa")); // Check sorting
        assertTrue(results.get(1).getClientName().contains("_bbb"));
    }
}