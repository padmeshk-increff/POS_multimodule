package com.increff.pos.unit.api;

import com.increff.pos.api.UserApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.UserDao;
import com.increff.pos.entity.User;
import com.increff.pos.model.enums.Role;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

// Import your factory methods
import static com.increff.pos.factory.UserFactory.mockNewObject;
import static com.increff.pos.factory.UserFactory.mockPersistedObject;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the UserApi class.
 * Mocks the UserDao to test validation and business logic.
 */
public class UserApiTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserApi userApi;

    @Before
    public void setUp() {
        // Initializes all @Mock and @InjectMocks fields
        MockitoAnnotations.openMocks(this);
    }

    // --- login() Tests ---

    @Test
    public void login_validCredentials_shouldReturnUser() throws ApiException {
        // Given
        String email = "test@example.com";
        String password = "password123";

        // This is the user as it exists in the database
        User existingUser = mockPersistedObject(email, password, Role.OPERATOR);

        // This is the user object coming from the login form
        User loginAttemptUser = new User();
        loginAttemptUser.setEmail(email);
        loginAttemptUser.setPassword(password);

        // Mock the internal getByEmail() call
        when(userDao.selectByEmail(email)).thenReturn(existingUser);

        // When
        User loggedInUser = userApi.login(loginAttemptUser);

        // Then
        assertNotNull(loggedInUser);
        // Your API's logic returns the *input* user, not the one from the DB
        assertEquals(loginAttemptUser, loggedInUser);
    }

    @Test
    public void login_invalidPassword_shouldThrowException() {
        // Given
        String email = "test@example.com";
        User existingUser = mockPersistedObject(email, "dbPassword", Role.SUPERVISOR);
        User loginAttemptUser = new User();
        loginAttemptUser.setEmail(email);
        loginAttemptUser.setPassword("wrongPassword");
        when(userDao.selectByEmail(email)).thenReturn(existingUser);

        // When/Then
        ApiException ex = assertThrows(ApiException.class,
            () -> userApi.login(loginAttemptUser)
        );
        assertEquals("Invalid password", ex.getMessage());
    }

    @Test
    public void login_userNotFound_shouldThrowException() {
        // Given
        String email = "notfound@example.com";
        User loginAttemptUser = new User();
        loginAttemptUser.setEmail(email);
        loginAttemptUser.setPassword("password123");
        when(userDao.selectByEmail(email)).thenReturn(null);

        // When/Then
        ApiException ex = assertThrows(ApiException.class,
            () -> userApi.login(loginAttemptUser)
        );
        assertEquals("User with email " + email + " doesn't exist", ex.getMessage());
    }

    // --- add() Tests ---

    @Test
    public void add_validNewUser_shouldSucceed() throws ApiException {
        // Given
        User newUser = mockNewObject("new@example.com", "password123");
        when(userDao.selectByEmail("new@example.com")).thenReturn(null);

        // When
        User addedUser = userApi.add(newUser);

        // Then
        assertNotNull(addedUser);
        assertEquals(newUser, addedUser);
    }

    @Test
    public void add_nullUser_shouldThrowException() {
        // When/Then
        ApiException ex = assertThrows(ApiException.class,
            () -> userApi.add(null)
        );
        assertEquals("User cannot be null", ex.getMessage());
    }

    @Test
    public void add_duplicateEmail_shouldThrowException() {
        // Given
        User newUser = mockNewObject("duplicate@example.com", "password123");
        User existingUser = mockPersistedObject("duplicate@example.com", "otherpass", Role.SUPERVISOR);
        when(userDao.selectByEmail("duplicate@example.com")).thenReturn(existingUser);

        // When/Then
        ApiException ex = assertThrows(ApiException.class,
            () -> userApi.add(newUser)
        );
        assertEquals("User with given email already exists", ex.getMessage());
    }

    // --- getByEmail() Tests ---

    @Test
    public void getCheckByEmail_existingUser_shouldReturnUser() throws ApiException {
        // Given
        String email = "test@example.com";
        User existingUser = mockPersistedObject(email, "password123", Role.SUPERVISOR);
        when(userDao.selectByEmail(email)).thenReturn(existingUser);

        // When
        User foundUser = userApi.getCheckByEmail(email);

        // Then
        assertNotNull(foundUser);
        assertEquals(existingUser, foundUser);
    }

    @Test
    public void getCheckByEmail_nullEmail_shouldThrowException() {
        // When/Then
        ApiException ex = assertThrows(ApiException.class,
            () -> userApi.getCheckByEmail(null)
        );
        assertEquals("Email cannot be null", ex.getMessage());
    }

    @Test
    public void getCheckByEmail_userNotFound_shouldThrowException() {
        // Given
        String email = "notfound@example.com";
        when(userDao.selectByEmail(email)).thenReturn(null);

        // When/Then
        ApiException ex = assertThrows(ApiException.class,
            () -> userApi.getCheckByEmail(email)
        );
        assertEquals("User with email " + email + " doesn't exist", ex.getMessage());
    }
}