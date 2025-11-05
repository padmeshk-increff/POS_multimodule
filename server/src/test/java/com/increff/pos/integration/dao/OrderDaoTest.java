package com.increff.pos.integration.dao;

import com.increff.pos.config.TestDbConfig;
import com.increff.pos.dao.OrderDao;
import com.increff.pos.entity.Order;
import com.increff.pos.factory.OrderFactory;
import com.increff.pos.model.enums.OrderStatus;
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

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestDbConfig.class)
@TestPropertySource("classpath:test.properties")
@Transactional
public class OrderDaoTest {

    @Autowired
    private OrderDao orderDao;

    // --- Tests for AbstractDao methods ---

    @Test
    public void testInsertAndSelectById() {
        // Arrange
        Order order = OrderFactory.mockNewObject();
        
        // Act
        orderDao.insert(order);
        
        // Assert
        assertNotNull(order.getId());
        Order fromDb = orderDao.selectById(order.getId());
        assertNotNull(fromDb);
        assertEquals(OrderStatus.CREATED, fromDb.getOrderStatus());
    }

    @Test
    public void testSelectAll() {
        // Arrange
        orderDao.insert(OrderFactory.mockNewObject());
        orderDao.insert(OrderFactory.mockNewObject());
        
        // Act
        List<Order> all = orderDao.selectAll();
        
        // Assert
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdate() {
        // Arrange
        Order order = OrderFactory.mockNewObject();
        orderDao.insert(order);
        
        // Act
        order.setOrderStatus(OrderStatus.INVOICED);
        order.setTotalAmount(500.0);
        orderDao.update(order);
        Order fromDb = orderDao.selectById(order.getId());
        
        // Assert
        assertEquals(OrderStatus.INVOICED, fromDb.getOrderStatus());
        assertEquals(500.0, fromDb.getTotalAmount(), 0.001);
    }

    @Test
    public void testDeleteById() {
        // Arrange
        Order order = OrderFactory.mockNewObject();
        orderDao.insert(order);
        Integer id = order.getId();
        
        // Act
        orderDao.deleteById(id);
        Order fromDb = orderDao.selectById(id);
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testInsertAllAndSelectByIds() {
        // Arrange
        List<Order> orders = Arrays.asList(
                OrderFactory.mockNewObject(),
                OrderFactory.mockNewObject()
        );
        
        // Act
        orderDao.insertAll(orders);
        List<Integer> ids = Arrays.asList(orders.get(0).getId(), orders.get(1).getId());
        List<Order> fromDb = orderDao.selectByIds(ids);
        
        // Assert
        assertEquals(2, fromDb.size());
    }

    // --- Tests for OrderDao specific methods ---

    @Test
    public void testSelectAllByDateRange() {
        // Arrange
        ZonedDateTime start = ZonedDateTime.now().minusDays(1);
        ZonedDateTime end = ZonedDateTime.now().plusDays(1);

        Order invoicedOrder = OrderFactory.mockNewObject();
        invoicedOrder.setOrderStatus(OrderStatus.INVOICED);
        orderDao.insert(invoicedOrder);
        
        Order createdOrder = OrderFactory.mockNewObject();
        createdOrder.setOrderStatus(OrderStatus.CREATED);
        orderDao.insert(createdOrder);
        
        // Act
        List<Order> results = orderDao.selectAllByDateRange(start, end);
        
        // Assert
        assertEquals(1, results.size()); // Only INVOICED orders
        assertTrue(results.stream().allMatch(o -> o.getOrderStatus() == OrderStatus.INVOICED));
    }

    @Test
    public void testFindWithFiltersNoFilter() {
        // Arrange
        orderDao.insert(OrderFactory.mockNewObject());
        orderDao.insert(OrderFactory.mockNewObject());
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        List<Order> results = orderDao.findWithFilters(null, null, null, null, pageable);
        Long count = orderDao.countWithFilters(null, null, null, null);
        
        // Assert
        assertTrue(results.size() >= 2);
        assertEquals(2, (long) count);
    }

    @Test
    public void testFindWithFiltersById() {
        // Arrange
        Order order = OrderFactory.mockNewObject();
        orderDao.insert(order);
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        List<Order> results = orderDao.findWithFilters(order.getId(), null, null, null, pageable);
        Long count = orderDao.countWithFilters(order.getId(), null, null, null);
        
        // Assert
        assertEquals(1L, (long) count);
        assertEquals(1, results.size());
        assertEquals(order.getId(), results.get(0).getId());
    }

    @Test
    public void testFindWithFiltersByStatus() {
        // Arrange
        Order createdOrder = OrderFactory.mockNewObject();
        createdOrder.setOrderStatus(OrderStatus.CREATED);
        orderDao.insert(createdOrder);
        
        Order invoicedOrder = OrderFactory.mockNewObject();
        invoicedOrder.setOrderStatus(OrderStatus.INVOICED);
        orderDao.insert(invoicedOrder);
        
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        List<Order> results = orderDao.findWithFilters(null, null, null, OrderStatus.CREATED, pageable);
        Long count = orderDao.countWithFilters(null, null, null, OrderStatus.CREATED);
        
        // Assert
        assertEquals(1, (long) count);
        assertTrue(results.stream().allMatch(o -> o.getOrderStatus() == OrderStatus.CREATED));
    }

    @Test
    public void testFindWithFiltersByDateRange() {
        // Arrange
        ZonedDateTime start = ZonedDateTime.now().minusHours(1);
        ZonedDateTime end = ZonedDateTime.now().plusHours(1);

        Order recentOrder = OrderFactory.mockNewObject();
        orderDao.insert(recentOrder);
        
        Pageable pageable = PageRequest.of(0, 100);
        
        // Act
        List<Order> results = orderDao.findWithFilters(null, start, end, null, pageable);
        Long count = orderDao.countWithFilters(null, start, end, null);
        
        // Assert
        assertEquals(1, (long) count);
        assertTrue(results.stream().anyMatch(o -> o.getId().equals(recentOrder.getId())));
    }

    @Test
    public void testFindWithFiltersPaginationAndSorting() {
        // Arrange
        orderDao.insert(OrderFactory.mockNewObject());
        orderDao.insert(OrderFactory.mockNewObject());
        orderDao.insert(OrderFactory.mockNewObject());
        
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());
        
        // Act
        List<Order> results = orderDao.findWithFilters(null, null, null, null, pageable);
        Long count = orderDao.countWithFilters(null, null, null, null);
        
        // Assert
        assertEquals(3, (long) count);
        assertEquals(2, results.size()); // Page size
        // Verify descending order
        assertTrue(results.get(0).getId() > results.get(1).getId());
    }
}
