package com.increff.pos.integration.dto;

import com.increff.pos.api.UserApi;
import com.increff.pos.config.SpringConfig;
import com.increff.pos.dto.SessionDto;
import com.increff.pos.dto.UserDto;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.commons.exception.FormValidationException;
import com.increff.pos.entity.User;
// We don't need UserFactory here because the form is simple enough to create
import com.increff.pos.model.data.LoginData;
import com.increff.pos.model.enums.Role;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.UserForm;
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
 * Integration Tests for the UserDto class.
 * This test specifically overrides the 'supervisor.emails' property
 * to create a predictable environment for role assignment testing.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@WebAppConfiguration
@TestPropertySource("classpath:test.properties")
@Transactional
public class UserDtoTest {

    @Autowired
    private UserDto userDto; // Class under test

    // --- Setup Dependencies ---
    @Autowired
    private UserApi userApi; // Used to verify data was saved

    @Autowired
    private SessionDto sessionDto; // Used to test login success after add

    /**
     * Helper to create a valid UserForm for tests.
     * (Corrected: Does not take a Role)
     */
    private UserForm createValidUserForm(String email, String password) {
        UserForm form = new UserForm();
        form.setEmail(email);
        form.setPassword(password);
        return form;
    }

    // --- add() Tests ---

    @Test
    public void add_validOperator_shouldSaveAsOperator() throws ApiException {
        // GIVEN
        String email = "operator@example.com"; // This email is NOT in the supervisor list
        String password = "Password#123";
        UserForm form = createValidUserForm(email, password);

        // WHEN
        Map<String, String> response = userDto.add(form);

        // THEN
        // 1. Check the response message
        assertNotNull(response);
        assertEquals("User signed up successfully", response.get("message"));

        // 2. Verify by fetching from the DB that the role is OPERATOR
        User user = userApi.getCheckByEmail(email);
        assertNotNull(user);
        assertEquals(email, user.getEmail());
        assertEquals(Role.OPERATOR, user.getRole());
    }

    @Test
    public void add_validSupervisor_shouldSaveAsSupervisor() throws ApiException {
        // GIVEN
        String email = "supervisor1@example.com"; // This email IS in the supervisor list
        String password = "Password#123";
        UserForm form = createValidUserForm(email, password);

        // WHEN
        Map<String, String> response = userDto.add(form);

        // THEN
        // 1. Check the response message
        assertNotNull(response);
        assertEquals("User signed up successfully", response.get("message"));

        // 2. Verify by fetching from the DB that the role is SUPERVISOR
        User user = userApi.getCheckByEmail(email);
        assertNotNull(user);
        assertEquals(email, user.getEmail());
        assertEquals(Role.SUPERVISOR, user.getRole());
    }

    @Test
    public void add_duplicateEmail_shouldThrowApiException() throws ApiException {
        // GIVEN
        String email = "test@example.com";
        // 1. Add the first user
        userDto.add(createValidUserForm(email, "Pass1#123"));

        // 2. Create a form for a second user with the same email
        UserForm duplicateForm = createValidUserForm(email, "Pass2#1234");

        // WHEN / THEN
        // This message comes from UserApi.add()
        ApiException ex = assertThrows(ApiException.class,
                () -> userDto.add(duplicateForm)
        );
        assertEquals("User with given email already exists", ex.getMessage());
    }

    // --- Validation Tests ---

    @Test
    public void add_blankEmail_shouldThrowValidationException() {
        // GIVEN
        // A blank email violates both @NotBlank and @Email
        UserForm form = createValidUserForm(" ", "Password#123");

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userDto.add(form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException fve = (FormValidationException) ex;
        Map<String, String> errors = fve.getErrors();

        // It has one key ("email") but two merged messages
        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("email"));

        // Check that both validation messages are present
        String errorMessage = errors.get("email");
        assertTrue(errorMessage.contains("Email cannot be blank"));
        assertTrue(errorMessage.contains("Please provide a valid email address"));
    }

    @Test
    public void add_invalidEmailFormat_shouldThrowValidationException() {
        // GIVEN
        UserForm form = createValidUserForm("not-an-email", "Password#123");

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userDto.add(form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException fve = (FormValidationException) ex;
        Map<String, String> errors = fve.getErrors();

        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("email"));
        assertEquals("Please provide a valid email address", errors.get("email"));
    }

    @Test
    public void add_blankPassword_shouldThrowValidationException() {
        // GIVEN
        UserForm form = createValidUserForm("test@example.com", " "); // Blank password

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userDto.add(form)
        );

        assertTrue("Exception was not a FormValidationException", ex instanceof FormValidationException);
        FormValidationException fve = (FormValidationException) ex;
        Map<String, String> errors = fve.getErrors();

        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("password"));

        // 2. Get the actual merged message
        String actualMessage = errors.get("password");

        // 3. Check that all expected error messages are present in the merged string
        assertTrue("Message should contain 'cannot be blank'",
                actualMessage.contains("Password cannot be blank"));

        assertTrue("Message should contain 'size' constraint",
                actualMessage.contains("Password must be between 8 and 100 characters"));

        assertTrue("Message should contain 'pattern' constraint",
                actualMessage.contains("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"));
    }
}