package com.increff.pos.unit.api;

import com.increff.pos.api.ClientApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.ClientDao;
import com.increff.pos.entity.Client;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.utils.BaseUtil;
import com.increff.pos.utils.ClientUtil; // Still need to mock this
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static com.increff.pos.factory.ClientFactory.mockNewObject;
import static com.increff.pos.factory.ClientFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Behavior-focused unit tests for the ClientApi class.
 * This test file validates the functional contract of the API
 * by mocking its DAO dependencies.
 */
public class ClientApiTest {

    @Mock
    private ClientDao clientDao;

    @InjectMocks
    private ClientApi clientApi; // The API class we are testing

    private Pageable testPageable;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testPageable = PageRequest.of(0, 5);
    }

    // --- insert() Tests ---

    @Test
    public void insert_validClient_shouldReturnClient() throws ApiException {
        // Given
        Client newClient = mockNewObject("new-client");
        when(clientDao.selectByName("new-client")).thenReturn(null);
        doNothing().when(clientDao).insert(newClient);

        // When
        Client savedClient = clientApi.insert(newClient);

        // Then
        // We only care that the method returned the object as expected.
        assertNotNull(savedClient);
        assertEquals("new-client", savedClient.getClientName());
    }

    @Test
    public void insert_nullClient_shouldThrowException() {
        // When/Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.insert(null)
        );
        assertEquals("Client object cannot be null", ex.getMessage());
    }

    @Test
    public void insert_clientWithId_shouldThrowException() {
        // Given
        Client clientWithId = mockPersistedObject(1, "test-client");

        // When/Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.insert(clientWithId)
        );
        assertEquals("Cannot insert with a pre-existing ID. ID must be null.", ex.getMessage());
    }

    @Test
    public void insert_duplicateName_shouldThrowException() {
        // Given
        Client newClient = mockNewObject("duplicate-name");
        Client existingClient = mockPersistedObject(1, "duplicate-name");
        when(clientDao.selectByName("duplicate-name")).thenReturn(existingClient);

        // When/Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.insert(newClient)
        );
        assertEquals("Client already exists", ex.getMessage());
    }

    // --- getCheckById() Tests ---

    @Test
    public void getCheckById_existingId_shouldReturnClient() throws ApiException {
        // Given
        Integer clientId = 1;
        Client expectedClient = mockPersistedObject(clientId, "test-client");
        when(clientDao.selectById(clientId)).thenReturn(expectedClient);

        // When
        Client actualClient = clientApi.getCheckById(clientId);

        // Then
        assertNotNull(actualClient);
        assertEquals(expectedClient, actualClient);
    }

    @Test
    public void getCheckById_nullId_shouldThrowException() {
        // When/Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.getCheckById(null)
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    @Test
    public void getCheckById_nonExistentId_shouldThrowException() {
        // Given
        when(clientDao.selectById(999)).thenReturn(null);

        // When/Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.getCheckById(999)
        );
        assertEquals("Client 999 doesn't exist", ex.getMessage());
    }

    // --- getById() Tests ---

    @Test
    public void getById_existingId_shouldReturnClient() throws ApiException {
        // Given
        Client expectedClient = mockPersistedObject(1);
        when(clientDao.selectById(1)).thenReturn(expectedClient);

        // When
        Client actualClient = clientApi.getById(1);

        // Then
        assertNotNull(actualClient);
        assertEquals(expectedClient, actualClient);
    }

    @Test
    public void getById_nonExistentId_shouldReturnNull() throws ApiException {
        // Given
        when(clientDao.selectById(999)).thenReturn(null);

        // When
        Client actualClient = clientApi.getById(999);

        // Then
        assertNull(actualClient);
    }

    @Test
    public void getById_nullId_shouldThrowException() {
        // When/Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.getById(null)
        );
        assertEquals("Id cannot be null", ex.getMessage());
    }

    // --- getCheckByName() Tests ---

    @Test
    public void getCheckByName_existingName_shouldReturnClient() throws ApiException {
        // Given
        Client expectedClient = mockPersistedObject(1, "test-client");
        when(clientDao.selectByName("test-client")).thenReturn(expectedClient);

        // When
        Client actualClient = clientApi.getCheckByName("test-client");

        // Then
        assertNotNull(actualClient);
        assertEquals(expectedClient, actualClient);
    }

    @Test
    public void getCheckByName_nonExistentName_shouldThrowException() {
        // Given
        when(clientDao.selectByName("fake-client")).thenReturn(null);

        // When/Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.getCheckByName("fake-client")
        );
        assertEquals("Client doesn't exist", ex.getMessage());
    }

    // --- getByName() Tests ---

    @Test
    public void getByName_nonExistentName_shouldReturnNull() throws ApiException {
        // Given
        when(clientDao.selectByName("fake-client")).thenReturn(null);
        // When
        Client actualClient = clientApi.getByName("fake-client");
        // Then
        assertNull(actualClient);
    }

    // --- getFilteredClients() Tests ---

    @Test
    public void getFilteredClients_clientsFound_shouldReturnPaginatedResult() throws ApiException {
        // Given
        List<Client> clientList = Collections.singletonList(mockPersistedObject(1, "test-client"));
        when(clientDao.countWithFilters("test")).thenReturn(10L);
        when(clientDao.selectWithFilters("test", testPageable)).thenReturn(clientList);

        // When
        PaginatedResult<Client> result = clientApi.getFilteredClients("test", testPageable);

        // Then
        assertNotNull(result);
        assertEquals(10L, (long) result.getTotalElements());
        assertEquals(2, (int) result.getTotalPages());
        assertEquals(clientList, result.getResults());
    }

    @Test
    public void getFilteredClients_noClientsFound_shouldReturnEmptyResult() throws ApiException {
        // Given
        when(clientDao.countWithFilters(null)).thenReturn(0L);

        try (MockedStatic<BaseUtil> mockedUtil = Mockito.mockStatic(BaseUtil.class)) {
            PaginatedResult<Client> emptyResult = new PaginatedResult<>();
            emptyResult.setResults(Collections.emptyList());
            emptyResult.setTotalElements(0L);
            emptyResult.setTotalPages(0);
            mockedUtil.when(BaseUtil::createEmptyResult).thenReturn(emptyResult);

            // When
            PaginatedResult<Client> result = clientApi.getFilteredClients(null, testPageable);

            // Then
            assertNotNull(result);
            assertEquals(emptyResult, result);
            assertTrue(result.getResults().isEmpty());
        }
    }

    @Test
    public void getFilteredClients_nullPageable_shouldThrowException() {
        // When/Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.getFilteredClients("test", null)
        );
        assertEquals("Pageable object cannot be null", ex.getMessage());
    }

    // --- updateById() Tests ---

    @Test
    public void updateById_validUpdate_shouldSucceed() throws ApiException {
        // Given
        Client clientUpdates = mockNewObject("updated-name");
        Client existingClient = mockPersistedObject(1, "original-name");

        when(clientDao.selectById(1)).thenReturn(existingClient);
        when(clientDao.selectByName("updated-name")).thenReturn(null);
        doNothing().when(clientDao).update(existingClient);

        // When
        Client updatedClient = clientApi.updateById(1, clientUpdates);

        // Then
        assertNotNull(updatedClient);
        assertEquals("updated-name", updatedClient.getClientName());
        assertEquals(existingClient, updatedClient); // Check it returns the modified object
    }

    @Test
    public void updateById_clientNotFound_shouldThrowException() {
        // Given
        Client clientUpdates = mockNewObject("updated-name");
        when(clientDao.selectById(999)).thenReturn(null);

        // When & Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.updateById(999, clientUpdates)
        );
        assertEquals("Client 999 doesn't exist", ex.getMessage());
    }

    @Test
    public void updateById_duplicateName_shouldThrowException() {
        // Given
        Client clientUpdates = mockNewObject("duplicate-name");
        Client existingClient = mockPersistedObject(1, "original-name");
        Client duplicateClient = mockPersistedObject(2, "duplicate-name"); // A different client

        when(clientDao.selectById(1)).thenReturn(existingClient);
        when(clientDao.selectByName("duplicate-name")).thenReturn(duplicateClient);

        // When & Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.updateById(1, clientUpdates)
        );
        assertEquals("Client duplicate-name already exists", ex.getMessage());
    }

    /**
     * This test validates the API's known logic bug.
     * It asserts that updating a client with its own name *fails*.
     */
    @Test
    public void updateById_sameName_shouldThrowException() {
        // Given
        Client clientUpdates = mockNewObject("same-name");
        Client existingClient = mockPersistedObject(1, "same-name");

        when(clientDao.selectById(1)).thenReturn(existingClient);
        // The duplicate check finds the *existing client itself*
        when(clientDao.selectByName("same-name")).thenReturn(existingClient);

        // When & Then
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.updateById(1, clientUpdates)
        );
        assertEquals("Client same-name already exists", ex.getMessage());
    }
}