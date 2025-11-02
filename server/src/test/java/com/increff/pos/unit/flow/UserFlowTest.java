package com.increff.pos.unit.flow;

import com.increff.pos.api.UserApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.flow.UserFlow;
import com.increff.pos.model.enums.Role;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

// Import your factory
import static com.increff.pos.factory.UserFactory.mockNewObject;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Behavior-focused unit tests for UserFlow.
 * Tests role assignment logic and password encoding.
 */
@RunWith(MockitoJUnitRunner.class)
public class UserFlowTest {

    @Mock
    private UserApi userApi;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserFlow userFlow;

    private String rawPassword = "rawPassword123";
    private String encodedPassword = "encodedPassword";
    private String supervisorEmail = "supervisor@example.com";
    private String operatorEmail = "operator@example.com";

    @Before
    public void setUp() {
        String supervisorList = supervisorEmail + ",admin@example.com";
        ReflectionTestUtils.setField(userFlow, "supervisorEmails", supervisorList);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
    }

    @Test
    public void add_supervisorEmail_shouldAssignSupervisorRole() throws ApiException {
        // GIVEN
        User newUser = mockNewObject(supervisorEmail, rawPassword);
        when(userApi.add(any(User.class))).thenReturn(newUser);

        // WHEN
        userFlow.add(newUser);

        // THEN - Test BEHAVIOR: correct role assigned and password encoded
        verify(userApi).add(argThat(user ->
            user.getRole() == Role.SUPERVISOR &&
            user.getPassword().equals(encodedPassword)
        ));
    }

    @Test
    public void add_regularEmail_shouldAssignOperatorRole() throws ApiException {
        // GIVEN
        User newUser = mockNewObject(operatorEmail, rawPassword);
        when(userApi.add(any(User.class))).thenReturn(newUser);

        // WHEN
        userFlow.add(newUser);

        // THEN - Test BEHAVIOR: correct role assigned and password encoded
        verify(userApi).add(argThat(user ->
            user.getRole() == Role.OPERATOR &&
            user.getPassword().equals(encodedPassword)
        ));
    }

    @Test
    public void add_duplicateEmail_shouldThrowException() throws ApiException {
        // GIVEN
        User newUser = mockNewObject(operatorEmail, rawPassword);
        doThrow(new ApiException("User already exists")).when(userApi).add(any());

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class, 
            () -> userFlow.add(newUser)
        );
        assertEquals("User already exists", ex.getMessage());
    }
}