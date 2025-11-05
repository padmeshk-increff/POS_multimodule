package com.increff.pos.integration.dao;

import com.increff.pos.config.TestDbConfig;
import com.increff.pos.dao.ClientDao;
import com.increff.pos.dao.OrderDao;
import com.increff.pos.dao.OrderItemDao;
import com.increff.pos.dao.ProductDao;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.factory.ClientFactory;
import com.increff.pos.factory.OrderFactory;
import com.increff.pos.factory.OrderItemFactory;
import com.increff.pos.factory.ProductFactory;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.result.ProductQuantityResult;
import com.increff.pos.model.result.SalesOverTimeResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestDbConfig.class)
@TestPropertySource("classpath:test.properties")
@Transactional
public class OrderItemDaoTest {

    @Autowired
    private OrderItemDao orderItemDao;
    
    @Autowired
    private OrderDao orderDao;
    
    @Autowired
    private ProductDao productDao;
    
    @Autowired
    private ClientDao clientDao;

    private Order testOrder1;
    private Order testOrder2;
    private Product testProduct1;
    private Product testProduct2;

    @Before
    public void setUp() {
        // Create prerequisite data
        Client client = ClientFactory.mockNewObject("test-client-" + System.currentTimeMillis());
        clientDao.insert(client);
        
        testProduct1 = ProductFactory.mockNewObject("ITEM-BC-1", client.getId());
        testProduct2 = ProductFactory.mockNewObject("ITEM-BC-2", client.getId());
        productDao.insert(testProduct1);
        productDao.insert(testProduct2);
        
        testOrder1 = OrderFactory.mockNewObject();
        testOrder2 = OrderFactory.mockNewObject();
        orderDao.insert(testOrder1);
        orderDao.insert(testOrder2);
    }

    // --- Tests for AbstractDao methods ---

    @Test
    public void testInsertAndSelectById() {
        // Arrange
        OrderItem item = OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct1.getId());
        
        // Act
        orderItemDao.insert(item);
        
        // Assert
        assertNotNull(item.getId());
        OrderItem fromDb = orderItemDao.selectById(item.getId());
        assertNotNull(fromDb);
        assertEquals(testOrder1.getId(), fromDb.getOrderId());
        assertEquals(testProduct1.getId(), fromDb.getProductId());
    }

    @Test
    public void testSelectAll() {
        // Arrange
        orderItemDao.insert(OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct1.getId()));
        orderItemDao.insert(OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct2.getId()));
        
        // Act
        List<OrderItem> all = orderItemDao.selectAll();
        
        // Assert
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdate() {
        // Arrange
        OrderItem item = OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct1.getId());
        orderItemDao.insert(item);
        
        // Act
        item.setQuantity(99);
        item.setSellingPrice(999.99);
        orderItemDao.update(item);
        OrderItem fromDb = orderItemDao.selectById(item.getId());
        
        // Assert
        assertEquals(99, (int) fromDb.getQuantity());
        assertEquals(999.99, fromDb.getSellingPrice(), 0.001);
    }

    @Test
    public void testDeleteById() {
        // Arrange
        OrderItem item = OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct1.getId());
        orderItemDao.insert(item);
        Integer id = item.getId();
        
        // Act
        orderItemDao.deleteById(id);
        OrderItem fromDb = orderItemDao.selectById(id);
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testInsertAllAndSelectByIds() {
        // Arrange
        List<OrderItem> items = Arrays.asList(
                OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct1.getId()),
                OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct2.getId())
        );
        
        // Act
        orderItemDao.insertAll(items);
        List<Integer> ids = Arrays.asList(items.get(0).getId(), items.get(1).getId());
        List<OrderItem> fromDb = orderItemDao.selectByIds(ids);
        
        // Assert
        assertEquals(2, fromDb.size());
    }

    // --- Tests for OrderItemDao specific methods ---

    @Test
    public void testSelectByOrderId() {
        // Arrange
        orderItemDao.insert(OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct1.getId()));
        orderItemDao.insert(OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct2.getId()));
        orderItemDao.insert(OrderItemFactory.mockNewObject(testOrder2.getId(), testProduct1.getId()));
        
        // Act
        List<OrderItem> items = orderItemDao.selectByOrderId(testOrder1.getId());
        
        // Assert
        assertEquals(2, items.size());
        assertTrue(items.stream().allMatch(i -> i.getOrderId().equals(testOrder1.getId())));
    }

    @Test
    public void testSelectByOrderIdAndProductIdFound() {
        // Arrange
        OrderItem item = OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct1.getId());
        orderItemDao.insert(item);
        
        // Act
        OrderItem fromDb = orderItemDao.selectByOrderIdAndProductId(testOrder1.getId(), testProduct1.getId());
        
        // Assert
        assertNotNull(fromDb);
        assertEquals(item.getId(), fromDb.getId());
    }

    @Test
    public void testSelectByOrderIdAndProductIdNotFound() {
        // Act
        OrderItem fromDb = orderItemDao.selectByOrderIdAndProductId(9999, 9999);
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testSelectByOrderIds() {
        // Arrange
        orderItemDao.insert(OrderItemFactory.mockNewObject(testOrder1.getId(), testProduct1.getId()));
        orderItemDao.insert(OrderItemFactory.mockNewObject(testOrder2.getId(), testProduct2.getId()));
        
        // Act
        List<OrderItem> items = orderItemDao.selectByOrderIds(Arrays.asList(testOrder1.getId(), testOrder2.getId()));
        
        // Assert
        assertEquals(2, items.size());
    }

    @Test
    public void testFindTopSellingProducts() {
        // Arrange
        ZonedDateTime start = ZonedDateTime.now().minusDays(1);
        ZonedDateTime end = ZonedDateTime.now().plusDays(1);
        
        // Create invoiced orders with items
        Order invoicedOrder = OrderFactory.mockNewObject();
        invoicedOrder.setOrderStatus(OrderStatus.INVOICED);
        orderDao.insert(invoicedOrder);
        
        OrderItem item1 = OrderItemFactory.mockNewObject(invoicedOrder.getId(), testProduct1.getId());
        item1.setQuantity(10);
        item1.setSellingPrice(100.0);
        orderItemDao.insert(item1);
        
        OrderItem item2 = OrderItemFactory.mockNewObject(invoicedOrder.getId(), testProduct2.getId());
        item2.setQuantity(5);
        item2.setSellingPrice(200.0);
        orderItemDao.insert(item2);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // Act
        List<ProductQuantityResult> results = orderItemDao.findTopSellingProducts(start, end, pageable, OrderStatus.INVOICED);
        
        // Assert
        assertTrue(results.size() >= 2);
        // Verify our products are in the results
        assertTrue(results.stream().anyMatch(r -> r.getProductId().equals(testProduct1.getId())));
        assertTrue(results.stream().anyMatch(r -> r.getProductId().equals(testProduct2.getId())));
    }

    @Test
    public void testFindTopSellingProductsOnlyInvoicedOrders() {
        // Arrange
        ZonedDateTime start = ZonedDateTime.now().minusDays(1);
        ZonedDateTime end = ZonedDateTime.now().plusDays(1);
        
        // Create CREATED order (should not be included)
        Order createdOrder = OrderFactory.mockNewObject();
        createdOrder.setOrderStatus(OrderStatus.CREATED);
        orderDao.insert(createdOrder);
        
        OrderItem createdItem = OrderItemFactory.mockNewObject(createdOrder.getId(), testProduct1.getId());
        createdItem.setQuantity(100);
        orderItemDao.insert(createdItem);

        // Create INVOICED order
        Order invoicedOrder = OrderFactory.mockNewObject();
        invoicedOrder.setOrderStatus(OrderStatus.INVOICED);
        orderDao.insert(invoicedOrder);
        
        OrderItem invoicedItem = OrderItemFactory.mockNewObject(invoicedOrder.getId(), testProduct2.getId());
        invoicedItem.setQuantity(10);
        orderItemDao.insert(invoicedItem);
        
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        List<ProductQuantityResult> results = orderItemDao.findTopSellingProducts(start, end, pageable, OrderStatus.INVOICED);
        
        // Assert
        assertEquals(1, results.size());
        // Should not include the CREATED order's product
        assertFalse(results.stream().anyMatch(r -> r.getProductId().equals(testProduct1.getId()) && r.getTotalQuantity() == 100));
    }

    @Test
    public void testFindSalesByDate() {
        // Arrange
        ZonedDateTime start = ZonedDateTime.now().minusDays(1);
        ZonedDateTime end = ZonedDateTime.now().plusDays(1);

        // Create invoiced order
        Order invoicedOrder = OrderFactory.mockNewObject();
        invoicedOrder.setOrderStatus(OrderStatus.INVOICED);
        orderDao.insert(invoicedOrder);
        
        OrderItem item = OrderItemFactory.mockNewObject(invoicedOrder.getId(), testProduct1.getId());
        item.setQuantity(5);
        item.setSellingPrice(100.0);
        orderItemDao.insert(item);
        
        // Act
        List<SalesOverTimeResult> results = orderItemDao.findSalesByDate(start, end, OrderStatus.INVOICED);

        double totalRevenue = results.stream().mapToDouble(SalesOverTimeResult::getRevenue).sum();
        assertTrue(totalRevenue >= 500.0); // Our item contributes 500
    }
}
