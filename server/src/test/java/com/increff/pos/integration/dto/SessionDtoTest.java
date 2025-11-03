package com.increff.pos.integration.dto;

import com.increff.pos.api.UserApi;
import com.increff.pos.config.SpringConfig;
import com.increff.pos.dto.SessionDto;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.commons.exception.FormValidationException;
import com.increff.pos.entity.User;
import com.increff.pos.factory.UserFactory; // Assuming this factory exists
import com.increff.pos.flow.UserFlow;
import com.increff.pos.model.data.LoginData;
import com.increff.pos.model.enums.Role;
import com.increff.pos.model.form.LoginForm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration Tests for the SessionDto class.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@WebAppConfiguration
@TestPropertySource("classpath:test.properties")
@Transactional
public class SessionDtoTest {

    @Autowired
    private SessionDto sessionDto;

    // --- Setup Dependencies ---
    @Autowired
    private UserFlow userFlow;

    // --- Prerequisite Data ---
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final Role TEST_ROLE = Role.OPERATOR;

    /**
     * Helper to create a valid LoginForm for tests.
     */
    private LoginForm createValidLoginForm(String email, String password) {
        LoginForm form = new LoginForm();
        form.setEmail(email);
        form.setPassword(password);
        return form;
    }

    /**
     * Sets up prerequisite data:
     * 1 User with a known email and password.
     * The UserApi is used to ensure the password is correctly hashed in the DB.
     */
    @Before
    public void setUp() throws ApiException {
        // 1. Create a transient User entity using the factory
        User newUser = UserFactory.mockNewObject(TEST_EMAIL, TEST_PASSWORD);

        // 2. Insert via UserApi, which will hash the password
        userFlow.add(newUser);
    }

    // --- login() Tests ---

    @Test
    public void login_validCredentials_shouldReturnLoginData() throws ApiException {
        // GIVEN
        LoginForm form = createValidLoginForm(TEST_EMAIL, TEST_PASSWORD);

        // WHEN
        LoginData loginData = sessionDto.login(form);

        // THEN
        assertNotNull(loginData);
        assertEquals(TEST_EMAIL, loginData.getEmail());
        assertEquals(Role.OPERATOR, loginData.getRole());
        assertNotNull(loginData.getToken());
        assertFalse(loginData.getToken().isEmpty());
    }

    @Test
    public void login_invalidPassword_shouldThrowApiException() {
        // GIVEN
        LoginForm form = createValidLoginForm(TEST_EMAIL, "wrongpassword");

        // WHEN / THEN
        // This message comes from SessionFlow
        ApiException ex = assertThrows(ApiException.class,
                () -> sessionDto.login(form)
        );
        assertEquals("Invalid credentials provided. Please check your email and password.", ex.getMessage());
    }

    @Test
    public void login_nonExistentUser_shouldThrowApiException() {
        // GIVEN
        LoginForm form = createValidLoginForm("nouser@example.com", TEST_PASSWORD);

        // WHEN / THEN
        // This message comes from UserApi.getCheckByEmail
        ApiException ex = assertThrows(ApiException.class,
                () -> sessionDto.login(form)
        );
        assertEquals("User with email nouser@example.com doesn't exist", ex.getMessage());
    }

    @Test
    public void login_blankEmail_shouldThrowValidationException() {
        // GIVEN
        LoginForm form = createValidLoginForm(" ", TEST_PASSWORD); // Blank email

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> sessionDto.login(form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException fve = (FormValidationException) ex;
        Map<String, String> errors = fve.getErrors();

        assertEquals(1, errors.size());
        String actualMessage = errors.get("email");
        assertTrue("Message should contain 'cannot be blank'",
                actualMessage.contains("Email cannot be blank"));
        assertTrue("Message should contain 'valid email address'",
                actualMessage.contains("Please provide a valid email address"));
    }

    @Test
    public void login_invalidEmailFormat_shouldThrowValidationException() {
        // GIVEN
        LoginForm form = createValidLoginForm("not-an-email", TEST_PASSWORD);

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> sessionDto.login(form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException fve = (FormValidationException) ex;
        Map<String, String> errors = fve.getErrors();

        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("email"));
        assertEquals("Please provide a valid email address", errors.get("email"));
    }

    @Test
    public void login_blankPassword_shouldThrowValidationException() {
        // GIVEN
        LoginForm form = createValidLoginForm(TEST_EMAIL, " "); // Blank password

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> sessionDto.login(form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException fve = (FormValidationException) ex;
        Map<String, String> errors = fve.getErrors();

        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("password"));
        assertEquals("Password cannot be blank", errors.get("password"));
    }
}