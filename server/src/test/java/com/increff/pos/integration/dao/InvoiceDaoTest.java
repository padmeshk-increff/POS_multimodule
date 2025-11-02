package com.increff.pos.integration.dao;

import com.increff.pos.config.TestDbConfig;
import com.increff.pos.dao.InvoiceDao;
import com.increff.pos.dao.OrderDao;
import com.increff.pos.entity.Invoice;
import com.increff.pos.entity.Order;
import com.increff.pos.factory.InvoiceFactory;
import com.increff.pos.factory.OrderFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
public class InvoiceDaoTest {

    @Autowired
    private InvoiceDao invoiceDao;
    
    @Autowired
    private OrderDao orderDao;

    private Order testOrder1;
    private Order testOrder2;

    @Before
    public void setUp() {
        // Create prerequisite orders
        testOrder1 = OrderFactory.mockNewObject();
        testOrder2 = OrderFactory.mockNewObject();
        orderDao.insert(testOrder1);
        orderDao.insert(testOrder2);
    }

    // --- Tests for AbstractDao methods ---

    @Test
    public void testInsertAndSelectById() {
        // Arrange
        Invoice invoice = InvoiceFactory.mockNewObject(testOrder1.getId());
        
        // Act
        invoiceDao.insert(invoice);
        
        // Assert
        assertNotNull(invoice.getId());
        Invoice fromDb = invoiceDao.selectById(invoice.getId());
        assertNotNull(fromDb);
        assertEquals(testOrder1.getId(), fromDb.getOrderId());
        assertNotNull(fromDb.getFilePath());
    }

    @Test
    public void testSelectAll() {
        // Arrange
        invoiceDao.insert(InvoiceFactory.mockNewObject(testOrder1.getId()));
        invoiceDao.insert(InvoiceFactory.mockNewObject(testOrder2.getId()));
        
        // Act
        List<Invoice> all = invoiceDao.selectAll();
        
        // Assert
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdate() {
        // Arrange
        Invoice invoice = InvoiceFactory.mockNewObject(testOrder1.getId());
        invoiceDao.insert(invoice);
        
        // Act
        String newPath = "/new/path/invoice-" + System.currentTimeMillis() + ".pdf";
        invoice.setFilePath(newPath);
        invoiceDao.update(invoice);
        Invoice fromDb = invoiceDao.selectById(invoice.getId());
        
        // Assert
        assertEquals(newPath, fromDb.getFilePath());
    }

    @Test
    public void testDeleteById() {
        // Arrange
        Invoice invoice = InvoiceFactory.mockNewObject(testOrder1.getId());
        invoiceDao.insert(invoice);
        Integer id = invoice.getId();
        
        // Act
        invoiceDao.deleteById(id);
        Invoice fromDb = invoiceDao.selectById(id);
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testInsertAllAndSelectByIds() {
        // Arrange
        List<Invoice> invoices = Arrays.asList(
                InvoiceFactory.mockNewObject(testOrder1.getId()),
                InvoiceFactory.mockNewObject(testOrder2.getId())
        );
        
        // Act
        invoiceDao.insertAll(invoices);
        List<Integer> ids = Arrays.asList(invoices.get(0).getId(), invoices.get(1).getId());
        List<Invoice> fromDb = invoiceDao.selectByIds(ids);
        
        // Assert
        assertEquals(2, fromDb.size());
    }

    // --- Tests for InvoiceDao specific methods ---

    @Test
    public void testSelectByOrderId_found() {
        // Arrange
        Invoice invoice = InvoiceFactory.mockNewObject(testOrder1.getId());
        invoiceDao.insert(invoice);
        
        // Act
        Invoice fromDb = invoiceDao.selectByOrderId(testOrder1.getId());
        
        // Assert
        assertNotNull(fromDb);
        assertEquals(invoice.getId(), fromDb.getId());
        assertEquals(testOrder1.getId(), fromDb.getOrderId());
    }

    @Test
    public void testSelectByOrderId_notFound() {
        // Act
        Invoice fromDb = invoiceDao.selectByOrderId(9999);
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testSelectByOrderId_uniqueConstraint() {
        // Arrange
        Invoice invoice1 = InvoiceFactory.mockNewObject(testOrder1.getId());
        invoiceDao.insert(invoice1);
        
        // Act
        Invoice fromDb = invoiceDao.selectByOrderId(testOrder1.getId());
        
        // Assert
        assertNotNull(fromDb);
        assertEquals(invoice1.getId(), fromDb.getId());
        
        // Note: If you try to insert another invoice for the same order,
        // it should fail due to unique constraint (if configured in DB)
        // This test just verifies we get the first one
    }

    @Test
    public void testSelectByOrderId_withCustomFilePath() {
        // Arrange
        String customPath = "/custom/invoices/order-" + testOrder1.getId() + ".pdf";
        Invoice invoice = InvoiceFactory.mockNewObject(testOrder1.getId());
        invoice.setFilePath(customPath);
        invoiceDao.insert(invoice);
        
        // Act
        Invoice fromDb = invoiceDao.selectByOrderId(testOrder1.getId());
        
        // Assert
        assertNotNull(fromDb);
        assertEquals(customPath, fromDb.getFilePath());
    }
}
