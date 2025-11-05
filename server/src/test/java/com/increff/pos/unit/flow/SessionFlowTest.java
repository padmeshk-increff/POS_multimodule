package com.increff.pos.unit.flow;

import com.increff.pos.api.UserApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.flow.SessionFlow;
import com.increff.pos.model.enums.Role;
import com.increff.pos.model.result.LoginResult;
import com.increff.pos.utils.JwtUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

// Import your factory
import static com.increff.pos.factory.UserFactory.mockPersistedObject;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Behavior-focused unit tests for SessionFlow.
 * Tests login flow and authentication logic.
 */
public class SessionFlowTest {

    @Mock
    private UserApi userApi;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private SessionFlow sessionFlow;

    private User loginFormUser;
    private User dbUser;
    private String rawPassword = "rawPassword123";
    private String encodedPassword = "encodedPassword";
    private String userEmail = "test@example.com";
    private String fakeToken = "fake.jwt.token";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        loginFormUser = new User();
        loginFormUser.setEmail(userEmail);
        loginFormUser.setPassword(rawPassword);

        dbUser = mockPersistedObject(userEmail, encodedPassword, Role.SUPERVISOR);
    }

    @Test
    public void loginValidCredentialsShouldReturnLoginResult() throws ApiException {
        // GIVEN
        when(userApi.getCheckByEmail(userEmail)).thenReturn(dbUser);
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtUtil.generateToken(dbUser)).thenReturn(fakeToken);

        // WHEN
        LoginResult result = sessionFlow.login(loginFormUser);

        // THEN - Test BEHAVIOR: correct result returned
        assertNotNull(result);
        assertEquals(dbUser, result.getUser());
        assertEquals(fakeToken, result.getToken());
    }

    @Test
    public void loginUserNotFoundShouldThrowException() throws ApiException {
        // GIVEN
        when(userApi.getCheckByEmail(userEmail))
                .thenThrow(new ApiException("User with given email does not exist"));

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> sessionFlow.login(loginFormUser)
        );
        assertEquals("User with given email does not exist", ex.getMessage());
    }

    @Test
    public void loginInvalidPasswordShouldThrowException() throws ApiException {
        // GIVEN
        when(userApi.getCheckByEmail(userEmail)).thenReturn(dbUser);
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        // WHEN/THEN
        ApiException ex = assertThrows(ApiException.class,
            () -> sessionFlow.login(loginFormUser)
        );
        assertEquals("Invalid credentials provided. Please check your email and password.", ex.getMessage());
    }

    @Test
    public void loginTokenGenerationFailsShouldThrowException() throws ApiException {
        // GIVEN
        when(userApi.getCheckByEmail(userEmail)).thenReturn(dbUser);
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtUtil.generateToken(dbUser)).thenThrow(new RuntimeException("Token signing key error"));

        // WHEN/THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> sessionFlow.login(loginFormUser)
        );
        assertEquals("Token signing key error", ex.getMessage());
    }
}