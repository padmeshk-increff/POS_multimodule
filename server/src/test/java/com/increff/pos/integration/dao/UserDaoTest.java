package com.increff.pos.integration.dao;

import com.increff.pos.config.TestDbConfig;
import com.increff.pos.dao.UserDao;
import com.increff.pos.entity.User;
import com.increff.pos.factory.UserFactory;
import com.increff.pos.model.enums.Role;
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
public class UserDaoTest {

    @Autowired
    private UserDao userDao;

    // --- Tests for AbstractDao methods ---

    @Test
    public void testInsertAndSelectById() {
        // Arrange
        String uniqueEmail = "test-" + System.currentTimeMillis() + "@example.com";
        User user = UserFactory.mockNewObject(uniqueEmail, "password123");
        user.setRole(Role.OPERATOR);
        
        // Act
        userDao.insert(user);
        
        // Assert
        assertNotNull(user.getId());
        User fromDb = userDao.selectById(user.getId());
        assertNotNull(fromDb);
        assertEquals(uniqueEmail, fromDb.getEmail());
        assertEquals(Role.OPERATOR, fromDb.getRole());
    }

    @Test
    public void testSelectAll() {
        // Arrange

        String email1 = "user1-" + System.currentTimeMillis() + "@example.com";
        String email2 = "user2-" + System.currentTimeMillis() + "@example.com";
        
        User user1 = UserFactory.mockNewObject(email1, "pass1");
        user1.setRole(Role.OPERATOR);
        User user2 = UserFactory.mockNewObject(email2, "pass2");
        user2.setRole(Role.SUPERVISOR);
        
        userDao.insert(user1);
        userDao.insert(user2);
        
        // Act
        List<User> all = userDao.selectAll();
        
        // Assert
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdate() {
        // Arrange
        String email = "update-" + System.currentTimeMillis() + "@example.com";
        User user = UserFactory.mockNewObject(email, "oldpass");
        user.setRole(Role.OPERATOR);
        userDao.insert(user);
        
        // Act
        user.setPassword("newpass");
        user.setRole(Role.SUPERVISOR);
        userDao.update(user);
        User fromDb = userDao.selectById(user.getId());
        
        // Assert
        assertEquals("newpass", fromDb.getPassword());
        assertEquals(Role.SUPERVISOR, fromDb.getRole());
    }

    @Test
    public void testDeleteById() {
        // Arrange
        String email = "delete-" + System.currentTimeMillis() + "@example.com";
        User user = UserFactory.mockNewObject(email, "password");
        user.setRole(Role.OPERATOR);
        userDao.insert(user);
        Integer id = user.getId();
        
        // Act
        userDao.deleteById(id);
        User fromDb = userDao.selectById(id);
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testInsertAllAndSelectByIds() {
        // Arrange
        String email1 = "batch1-" + System.currentTimeMillis() + "@example.com";
        String email2 = "batch2-" + System.currentTimeMillis() + "@example.com";
        
        User user1 = UserFactory.mockNewObject(email1, "pass1");
        user1.setRole(Role.OPERATOR);
        User user2 = UserFactory.mockNewObject(email2, "pass2");
        user2.setRole(Role.SUPERVISOR);
        
        List<User> users = Arrays.asList(user1, user2);
        
        // Act
        userDao.insertAll(users);
        List<Integer> ids = Arrays.asList(user1.getId(), user2.getId());
        List<User> fromDb = userDao.selectByIds(ids);
        
        // Assert
        assertEquals(2, fromDb.size());
    }

    // --- Tests for UserDao specific methods ---

    @Test
    public void testSelectByEmail_found() {
        // Arrange
        String uniqueEmail = "unique-" + System.currentTimeMillis() + "@example.com";
        User user = UserFactory.mockNewObject(uniqueEmail, "password");
        user.setRole(Role.OPERATOR);
        userDao.insert(user);
        
        // Act
        User fromDb = userDao.selectByEmail(uniqueEmail);
        
        // Assert
        assertNotNull(fromDb);
        assertEquals(user.getId(), fromDb.getId());
        assertEquals(uniqueEmail, fromDb.getEmail());
    }

    @Test
    public void testSelectByEmail_notFound() {
        // Act
        User fromDb = userDao.selectByEmail("nonexistent-" + System.currentTimeMillis() + "@example.com");
        
        // Assert
        assertNull(fromDb);
    }

    @Test
    public void testSelectByEmail_caseInsensitive() {
        // Arrange
        String email = "CaseSensitive-" + System.currentTimeMillis() + "@Example.COM";
        User user = UserFactory.mockNewObject(email, "password");
        user.setRole(Role.OPERATOR);
        userDao.insert(user);
        
        // Act
        User fromDb = userDao.selectByEmail(email.toLowerCase());
        
        // Assert
        assertNotNull(fromDb);
    }
}
